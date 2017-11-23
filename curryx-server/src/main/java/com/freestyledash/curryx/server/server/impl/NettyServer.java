package com.freestyledash.curryx.server.server.impl;

import com.freestyledash.curryx.common.protocol.codec.RPCDecoder;
import com.freestyledash.curryx.common.protocol.codec.RPCEncoder;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.server.server.Server;
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

import java.util.Map;

/**
 * @author zhangyanqi
 * @since 1.0 2017/11/23
 */
public class NettyServer implements Server {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);


    /**
     * 被注册为服务的集合
     */
    private Map serviceMap;

    /**
     * netty监听的服务器网络进程地址
     */
    private String serverAddress;

    /**
     * boss工作线程个数,默认为1
     */
    private int bossThreadCount = 1;

    /**
     * worker工作线程个数，默认为1
     */
    private int workerThreadCount = 1;

    public NettyServer(Map serviceMap, String serverAddress) {
        this.serviceMap = serviceMap;
        this.serverAddress = serverAddress;
    }

    public NettyServer(Map serviceMap, String serverAddress, int bossThreadCount, int workerThreadCount) {
        this.serviceMap = serviceMap;
        this.serverAddress = serverAddress;
        this.bossThreadCount = bossThreadCount;
        this.workerThreadCount = workerThreadCount;
        if (bossThreadCount < 1 || workerThreadCount < 1) {
            throw new RuntimeException("Netty服务器参数设置不正确");
        }
    }

    /**
     * 服务器启动
     */
    @Override
    public synchronized void  start() {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(this.bossThreadCount);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(this.workerThreadCount);

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

    }
}
