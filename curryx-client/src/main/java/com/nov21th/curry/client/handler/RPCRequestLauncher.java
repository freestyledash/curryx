package com.nov21th.curry.client.handler;

import com.nov21th.curry.common.protocol.codec.RPCDecoder;
import com.nov21th.curry.common.protocol.codec.RPCEncoder;
import com.nov21th.curry.common.protocol.entity.RPCRequest;
import com.nov21th.curry.common.protocol.entity.RPCResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/4.
 */
public class RPCRequestLauncher extends SimpleChannelInboundHandler<RPCResponse> {

    private static final Logger logger = LoggerFactory.getLogger(RPCRequestLauncher.class);

    private String host;

    private int port;

    private RPCResponse response;

    public RPCRequestLauncher(String host, int port) {
        this.host = host;
        this.port = port;
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCResponse response) throws Exception {
        this.response = response;

        logger.debug("收到服务器响应：{}", response.getRequestId());
    }

    public RPCResponse launch(RPCRequest request) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RPCEncoder(RPCRequest.class))
                                    .addLast(new RPCDecoder(RPCResponse.class))
                                    .addLast(RPCRequestLauncher.this);
                        }
                    })
                    .option(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.connect(host, port).sync();

            logger.debug("连接到服务器：{}", host + ":" + port);
            logger.debug("发送请求：{}", request.getRequestId());

            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();

            return response;
        } finally {
            group.shutdownGracefully();
        }
    }

}
