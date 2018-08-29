package com.freestyledash.curryx.server.handler;

import com.freestyledash.curryx.common.interceptor.Advice;
import com.freestyledash.curryx.common.interceptor.impl.CalculateExecutTimeAdvice;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.serviceContainer.ServiceContainer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.freestyledash.curryx.common.constant.PunctuationConst.STRIGULA;

/**
 * 处理RPC请求的类
 * 利用了netty的InboundHandler
 * 处理的请求由其之前的InboundHandler即RPC解码器解码得到
 * 根据反射机制调用相应方法并将调用的结果或者发生的异常写入到RPC响应
 *
 * @author zhangyanqi
 */
@SuppressWarnings("ALL")
public class RPCRequestHandler extends SimpleChannelInboundHandler<RPCRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCRequestHandler.class);

    /**
     * 保存服务容器
     */
    private ServiceContainer serviceContainer;

    /**
     * aop通知对象
     */
    private List<Advice> advices;


    public RPCRequestHandler(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
        this.advices = new ArrayList<>();
        this.advices.add(new CalculateExecutTimeAdvice());
    }

    public RPCRequestHandler(ServiceContainer serviceContainer, List<Advice> advices) {
        this(serviceContainer);
        this.advices = advices;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, final RPCRequest request) throws Exception {
        LOGGER.debug("请求处理开始：{}", request.getRequestId());
        RPCResponse response = new RPCResponse();
        response.setRequestId(request.getRequestId());
        Object result = null;
        boolean processOk = true;
        try {
            result = handleRequest(request);
        } catch (Exception e) {
            LOGGER.error("请求处理({})过程中框架出错", request.getRequestId(), result);
            response.setException(e);
            processOk = false;
        }
        if (processOk && Exception.class.isAssignableFrom(result.getClass())) {
            //业务代码出现异常
            LOGGER.error("请求处理({})过程中业务代码出错", request.getRequestId(), result);
            response.setException((Exception) result);
        }
        response.setResult(result);
        context.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                channelFuture.channel().close();
                LOGGER.debug("请求处理完毕：{}", request.getRequestId());
            }
        });
    }

    /**
     * 处理请求并返回返回值
     *
     * @param request 请求
     * @return 方法返回值
     * @throws Exception
     */
    private Object handleRequest(RPCRequest request) throws Exception {
        for (Advice a : advices) {
            a.before();
        }
        String serviceFullName = request.getServiceName() + STRIGULA + request.getServiceVersion();
        Object serviceBean = serviceContainer.get(serviceFullName);
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
        Object result = null;
        try {
            result = method.invoke(serviceBean, request.getArgsValues());
        } catch (Exception e) {
            for (Advice a : advices) {
                a.encounterException(e);
            }
            return e;
        }
        return result;
    }
}
