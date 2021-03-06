package com.freestyledash.curryx.server.server.impl;

import com.freestyledash.curryx.common.addressTools.GetAddressTool;
import com.freestyledash.curryx.common.addressTools.impl.GetIpv4AddressByTraverseInterface;
import com.freestyledash.curryx.common.protocol.codec.RPCDecoder;
import com.freestyledash.curryx.common.protocol.codec.RPCEncoder;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.server.handler.RPCRequestHandler;
import com.freestyledash.curryx.server.server.Server;
import com.freestyledash.curryx.serviceContainer.ServiceContainer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static com.freestyledash.curryx.common.constant.ServerConst.DEFAULT_SERVER_PORT;

/**
 * 使用netty实现的服务器
 *
 * @author zhangyanqi
 * @since 1.0 2017/11/23
 */
public class NettyServer implements Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    /**
     * 被注册为服务的集合
     */
    private ServiceContainer serviceContainer;

    @Override
    public void setServiceContainer(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
    }

    /**
     * netty监听的服务器网络进程ip
     */
    private String ip;

    /**
     * 端口
     */
    private int port;

    /**
     * boss工作线程个数
     */
    private int bossThreadCount;

    /**
     * worker工作线程个数
     */
    private int workerThreadCount;


    public NettyServer(String ip, int port, int bossThreadCount, int workerThreadCount) {
        this.ip = ip;
        this.port = port;
        this.bossThreadCount = bossThreadCount;
        this.workerThreadCount = workerThreadCount;
        if (bossThreadCount < 1 || workerThreadCount < 1) {
            throw new IllegalArgumentException("Netty线程数量参数设置不正确");
        }
    }

    public NettyServer(String ip, int port) {
        this(ip, port, 1, 1);
    }

    public NettyServer(GetAddressTool tool, int prot, int bossThreadCount, int workerThreadCount) {
        this(tool.getAddress(), prot, bossThreadCount, workerThreadCount);
    }

    public NettyServer() {
        this(new GetIpv4AddressByTraverseInterface(), DEFAULT_SERVER_PORT, 1, 1);
    }

    public NettyServer(int bossThreadCount, int workerThreadCount) {
        this(new GetIpv4AddressByTraverseInterface(), DEFAULT_SERVER_PORT, bossThreadCount, workerThreadCount);
    }

    /**
     * @return 服务器监听的ip  例如:127.0.0.1
     */
    @Override
    public String getIp() {
        return ip;
    }

    /**
     * @return 服务器监听的端口 例如 80
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * @return 服务器监听的完整地址
     */
    @Override
    public String getAddress() {
        return ip + ":" + port;
    }

    /**
     * 通道
     */
    private Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 服务器启动
     */
    @Override
    public void start(CountDownLatch latch) {
        LOGGER.info("开始启动netty");
        final EventLoopGroup bossGroup = new NioEventLoopGroup(this.bossThreadCount);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(this.workerThreadCount);
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
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
                                    .addLast(new RPCRequestHandler(serviceContainer)); //第二个InboundHandler，用于处理RPC请求并生成RPC响应
                        }
                    });
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            LOGGER.info("netty成功启动(端口号:{})", port);
            latch.countDown();
            Channel channel = future.channel();
            this.channel = channel;
            ChannelFuture sync = channel.closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("启动服务器过程中发生异常", e);
        } finally {
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("开始关闭通讯服务");
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        LOGGER.info("通讯服务器已关闭");
    }

    /**
     * @return 服务器健康状态
     */
    @Override
    public boolean checkHealth() {
        LOGGER.info("开始服务健康检查");
        if (bossGroup == null || workerGroup == null || bossGroup.isShutdown() || workerGroup.isShutdown()) {
            LOGGER.error("服务不正常");
            return false;
        } else {
            LOGGER.info("服务器工作正常");
            return true;
        }
    }
}
