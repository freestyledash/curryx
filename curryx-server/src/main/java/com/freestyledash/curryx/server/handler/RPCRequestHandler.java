package com.freestyledash.curryx.server.handler;

import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.registry.constant.Constants;
import com.freestyledash.curryx.server.RPCServer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理RPC请求的类
 * <p>
 * 利用了netty的InboundHandler
 * 处理的请求由其之前的InboundHandler即RPC解码器解码得到
 * 根据反射机制调用相应方法并将调用的结果或者发生的异常写入到RPC响应
 *
 */
public class RPCRequestHandler extends SimpleChannelInboundHandler<RPCRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RPCServer.class);

    /**
     * 保存服务bean的map
     */
    private Map<String, Object> serviceMap = new HashMap();

    public RPCRequestHandler(Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;
    }

    protected void channelRead0(ChannelHandlerContext context, final RPCRequest request) throws Exception {
        logger.debug("请求处理开始：{}", request.getRequestId());

        RPCResponse response = new RPCResponse();
        response.setRequestId(request.getRequestId());

        try {
            Object result = handleRequest(request);
            response.setResult(result);
        } catch (Exception e) {
            logger.error("请求处理({})过程中出错", request.getRequestId(), e);
            response.setException(e);
        }

        context.writeAndFlush(response).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                channelFuture.channel().close();

                logger.debug("请求处理完毕：{}", request.getRequestId());
            }
        });
    }

    private Object handleRequest(RPCRequest request) throws Exception {
        String serviceFullName = request.getServiceName() + Constants.SERVICE_SEP + request.getServiceVersion();

        Object serviceBean = serviceMap.get(serviceFullName);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("未找到与(%s)相对应的服务", serviceFullName));
        }

        Class<?> clazz = serviceBean.getClass();

        //request对象中使用一个object数组来存储请求的参数,在反序列化的过程中不为null的都会被排在前面
        Object[] args = request.getArgsValues();
        if (args != null && args.length > 0) {
            Object[] temp = new Object[args.length];
            boolean[] nonNull = request.getNonNullArgs();

            int index = 0;
            for (int i = 0; i < args.length; i++) {
                temp[i] = nonNull[i] ? args[index++] : null;
            }

            request.setArgsValues(temp);
        }

        Method method = clazz.getMethod(request.getMethodName(), request.getArgsTypes());
        return method.invoke(serviceBean, request.getArgsValues());
    }
}
