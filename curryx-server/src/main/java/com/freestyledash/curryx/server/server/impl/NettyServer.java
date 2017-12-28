package com.freestyledash.curryx.server.server.impl;

import com.freestyledash.curryx.common.protocol.codec.RPCDecoder;
import com.freestyledash.curryx.common.protocol.codec.RPCEncoder;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.registry.util.constant.Constants;
import com.freestyledash.curryx.server.annotation.Service;
import com.freestyledash.curryx.server.handler.RPCRequestHandler;
import com.freestyledash.curryx.server.server.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 使用netty实现的服务器
 *
 * @author zhangyanqi
 * @since 1.0 2017/11/23
 */
public class NettyServer implements Server, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    /**
     * 被注册为服务的集合
     */
    private Map serviceMap = new HashMap();

    /**
     * netty监听的服务器网络进程地址
     */
    private String serverListeningAddress;

    /**
     * boss工作线程个数,默认为1
     */
    private int bossThreadCount = 1;

    /**
     * worker工作线程个数，默认为1
     */
    private int workerThreadCount = 1;

    public NettyServer(String serverAddress) {
        this.serverListeningAddress = serverAddress;
    }

    public NettyServer(String serverListeningAddress, int bossThreadCount, int workerThreadCount) {
        this.serverListeningAddress = serverListeningAddress;
        this.bossThreadCount = bossThreadCount;
        this.workerThreadCount = workerThreadCount;
        if (bossThreadCount < 1 || workerThreadCount < 1) {
            throw new RuntimeException("Netty服务器参数设置不正确");
        }
    }

    /**
     * 在spring初始化该类对象完成之后运行，用于等级被注册的服务
     *
     * @param context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        //扫描指定路径下被Service注解修饰的类
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
        //若扫描到的map为空则说明当前服务器没有提供任何服务，警告
        if (map == null || map.size() == 0) {
            return;
        }
        //对扫描到的每一个service，记录其服务名称和版本
        for (Object serviceBean : map.values()) {
            Service serviceAnnotation = serviceBean.getClass().getAnnotation(Service.class);
            String serviceFullName = serviceAnnotation.name().getName() + Constants.SERVICE_SEP + serviceAnnotation.version();
            serviceMap.put(serviceFullName, serviceBean);
        }
    }

    /**
     * 服务器启动
     */
    @Override
    public synchronized void start(CountDownLatch latch) {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(this.bossThreadCount);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(this.workerThreadCount);
        /**
         * 在jvm退出时，确保netty服务器安全退出，注意退出是指ctrl+c或者kill -15，如果用kill -9 那是没办法的
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!workerGroup.isShutdown()) {
                workerGroup.shutdownGracefully();
            }
            if (!bossGroup.isShutdown()) {
                bossGroup.shutdownGracefully();
            }
            logger.debug("HOOK：RPC服务器已关闭");
        }) {
        });

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //服务端处理客户端连接请求是顺序处理的，所以同一时间只能处理一个客户端连接，
                    //多个客户端来的时候，服务端将不能处理的客户端连接请求放在队列中等待处理，backlog参数指定了队列的大小
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    //如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RPCEncoder(RPCResponse.class))   //第一个OutboundHandler，用于编码RPC响应
                                    .addLast(new RPCDecoder(RPCRequest.class))  //第一个InboundHandler，用于解码RPC请求
                                    .addLast(new RPCRequestHandler(serviceMap)); //第二个InboundHandler，用于处理RPC请求并生成RPC响应
                        }
                    });

            String[] address = serverListeningAddress.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);

            ChannelFuture future = bootstrap.bind(host, port).sync();

            logger.debug("服务器已启动（端口号：{}）", port);

            latch.countDown(); //将栅栏-1

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("启动服务器过程中发生异常", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @Override
    public synchronized void shutdown() {
        logger.info("netty服务器关闭");
    }
}
