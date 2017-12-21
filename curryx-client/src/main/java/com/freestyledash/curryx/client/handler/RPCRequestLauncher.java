package com.freestyledash.curryx.client.handler;

import com.freestyledash.curryx.common.protocol.codec.RPCDecoder;
import com.freestyledash.curryx.common.protocol.codec.RPCEncoder;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端使用nio和服务器通讯
 * 每次通讯都创建一个新的RPCRequestLauncher对象
 */
public class RPCRequestLauncher extends SimpleChannelInboundHandler<RPCResponse> {

    private static final Logger logger = LoggerFactory.getLogger(RPCRequestLauncher.class);

    //目标ip地址
    private String host;
    //目标端口
    private int port;
    //请求响应
    private RPCResponse response;

    public RPCRequestLauncher(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 获得相应内容
     *
     * @param channelHandlerContext channel上下文
     * @param response              RPC响应对象
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCResponse response) throws Exception {
        this.response = response;
        logger.debug("收到服务器响应：{}", response.getRequestId());
    }

    /**
     * 启动服务器发送请求
     *
     * @param request 请求实体
     * @return 响应
     * @throws Exception
     */
    public RPCResponse launch(RPCRequest request) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    //指定创建连接类型
                    .channel(NioSocketChannel.class)
                    //立即发送
                    .option(ChannelOption.TCP_NODELAY, true)
                    //设置消息处理
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RPCEncoder())
                                    .addLast(new RPCDecoder())
                                    .addLast(RPCRequestLauncher.this);
                        }
                    });
            //同步等待连接，连接得到之后再继续
            ChannelFuture future = bootstrap.connect(host, port).sync();
            logger.debug("连接到服务器：{}", host + ":" + port);
            logger.debug("发送请求：{}", request.getRequestId());
            //连接成功
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }

}
