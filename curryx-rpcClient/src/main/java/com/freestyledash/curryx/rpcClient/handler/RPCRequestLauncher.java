package com.freestyledash.curryx.rpcClient.handler;

import com.freestyledash.curryx.common.protocol.codec.RPCDecoder;
import com.freestyledash.curryx.common.protocol.codec.RPCEncoder;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * rpc请求发起
 *
 * @author zhangyanqi
 */
@SuppressWarnings("ALL")
public class RPCRequestLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCRequestLauncher.class);

    /**
     * netty启动类
     */
    private Bootstrap bootstrap = null;

    /**
     * 响应处理类
     */
    private RPCResponseHandler handler = null;


    public RPCRequestLauncher(int eventLoopThreadCount) {
        RPCResponseHandler handler = new RPCResponseHandler();
        this.handler = handler;
        NioEventLoopGroup group = new NioEventLoopGroup(eventLoopThreadCount);
        try {
            bootstrap = new Bootstrap();
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
                                    .addLast(new RPCEncoder(RPCRequest.class))
                                    .addLast(new RPCDecoder(RPCResponse.class))
                                    .addLast(handler);
                        }
                    });
        } catch (Exception e) {
            group.shutdownGracefully();
            throw new RuntimeException(e);
        }
        //在jvm退出时确保group退出
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!group.isShutdown()) {
                group.shutdownGracefully();
            }
            LOGGER.debug("HOOK：RPC服务器已关闭");
        }, "NettyShutdownHook") {
        });
    }


    /**
     * 启动服务器发送请求
     *
     * @param host    ip地址
     * @param port    端口
     * @param request 请求实体
     * @return 响应
     * @throws Exception 异常
     */
    public RPCResponse launch(String host, int port, RPCRequest request) throws Exception {
        //同步等待连接，连接得到之后再继续
        ChannelFuture future = bootstrap.connect(host, port).sync();
        LOGGER.debug("连接到服务器：{}", host + ":" + port);
        LOGGER.debug("发送请求：{}", request.getRequestId());
        //连接成功
        Channel channel = future.channel();
        channel.writeAndFlush(request).sync();
        channel.closeFuture().sync();
        return handler.getResponse(request.getRequestId());
    }
}
