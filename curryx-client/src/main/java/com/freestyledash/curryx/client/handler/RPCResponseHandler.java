package com.freestyledash.curryx.client.handler;

import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * netty客户端处理server响应的类
 *
 * @author zhangyanqi
 * @since 1.0 2017/12/27
 */

@SuppressWarnings("ALL")
@ChannelHandler.Sharable
public class RPCResponseHandler extends SimpleChannelInboundHandler<RPCResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCResponseHandler.class.getClass());

    /**
     * 响应
     */
    private volatile ConcurrentHashMap<String, RPCResponse> responseMap = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCResponse response) throws Exception {
        String requestId = response.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            channelHandlerContext.close();
            LOGGER.error("无效的请求id");
            return;
        }
        this.responseMap.put(requestId, response);
        LOGGER.debug("收到服务器响应：{}", response.getRequestId());
    }

    /**
     * 获得响应
     *
     * @param requestId 请求id
     * @return rpc响应
     */
    public RPCResponse getResponse(String requestId) {
        RPCResponse rpcResponse = responseMap.get(requestId);
        responseMap.remove(requestId);
        return rpcResponse;
    }
}
