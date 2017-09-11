package com.freestyledash.curryx.server;

import com.freestyledash.curryx.common.protocol.codec.RPCDecoder;
import com.freestyledash.curryx.common.protocol.codec.RPCEncoder;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.registry.constant.Constants;
import com.freestyledash.curryx.server.annotation.Service;
import com.freestyledash.curryx.server.handler.RPCRequestHandler;
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

/**
 * RPC服务端
 *
 * @author 郭永辉
 * @since 1.0 2017/4/4.
 */
public class RPCServer implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RPCServer.class);

    /**
     * 服务器的地址，格式为ip:port
     */
    private String serverAddress;

    /**
     * 注册服务的接口
     */
    private ServiceRegistry serviceRegistry;

    /**
     * 保存服务bean的map
     */
    private Map<String, Object> serviceMap = new HashMap();

    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        //扫描指定路径下被Service注解修饰的类
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
        //若扫描到的map为空则说明当前服务器没有提供任何服务，抛出异常
        if (map == null || map.size() == 0) {
            return;
        }

        //对扫描到的每一个service bean，记录其服务名称和版本
        for (Object serviceBean : map.values()) {
            Service serviceAnnotation = serviceBean.getClass().getAnnotation(Service.class);

            String serviceFullname = serviceAnnotation.name().getName() + Constants.SERVICE_SEP + serviceAnnotation.version();
            serviceMap.put(serviceFullname, serviceBean);

            logger.debug("扫描到服务：{}", serviceFullname);
        }
    }

    public void start() {
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (!workerGroup.isShutdown()) {
                    workerGroup.shutdownGracefully();
                }
                if (!bossGroup.isShutdown()) {
                    bossGroup.shutdownGracefully();
                }
                logger.debug("HOOK：RPC服务器已关闭");
            }
        });

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RPCEncoder(RPCResponse.class))    //第一个OutboundHandler，用于编码RPC响应
                                    .addLast(new RPCDecoder(RPCRequest.class))  //第一个InboundHandler，用于解码RPC请求
                                    .addLast(new RPCRequestHandler(serviceMap)); //第二个InboundHandler，用于处理RPC请求并生成RPC响应
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] address = serverAddress.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);

            ChannelFuture future = bootstrap.bind(host, port).sync();

            logger.debug("服务器已启动（端口号：{}）", port);

            registerServices();

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("启动服务器过程中发生异常", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private void registerServices() {
        if (serviceRegistry != null) {
            for (String serviceFullName : serviceMap.keySet()) {
                logger.debug("向注册中心注册服务：{}", serviceFullName);
                serviceRegistry.registerService(serviceFullName, serverAddress);
            }
        } else {
            throw new RuntimeException("服务中心不可用");
        }
    }

}
