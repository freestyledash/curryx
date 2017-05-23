package com.nov21th.curry.client;

import com.nov21th.curry.client.handler.RPCRequestLauncher;
import com.nov21th.curry.common.protocol.entity.RPCRequest;
import com.nov21th.curry.common.protocol.entity.RPCResponse;
import com.nov21th.curry.common.util.StringUtil;
import com.nov21th.curry.registry.ServiceDiscovery;
import com.nov21th.curry.registry.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * RPC客户端
 *
 * @author 郭永辉
 * @since 1.0 2017/4/4.
 */
public class RPCClient {

    private static final Logger logger = LoggerFactory.getLogger(RPCClient.class);

    /**
     * 发现服务的接口
     */
    private ServiceDiscovery serviceDiscovery;

    /**
     * 代理对象的缓存
     */
    private Map<String, Object> cachedProxy;

    public RPCClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;

        cachedProxy = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> clazz, final String version) {
        final String serviceFullname = clazz.getName() + Constants.SERVICE_SEP + version;

        Object proxy;
        //这里用双重校验锁保证对于每个serviceFullname内存中有唯一的代理实例与之对应
        if ((proxy = cachedProxy.get(serviceFullname)) == null) {
            synchronized (this) {
                if ((proxy = cachedProxy.get(serviceFullname)) == null) {
                    proxy = Proxy.newProxyInstance(
                            clazz.getClassLoader(),
                            new Class<?>[]{clazz},
                            new InvocationHandler() {
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    long requestStartTime = System.currentTimeMillis();

                                    RPCRequest request = new RPCRequest();

                                    request.setServiceName(clazz.getName());
                                    request.setServiceVersion(version);
                                    request.setMethodName(method.getName());
                                    request.setArgsTypes(method.getParameterTypes());
                                    request.setArgsValues(args);

                                    String node;
                                    String serverAddress;
                                    if (serviceDiscovery != null) {
                                        logger.debug("向服务中心查询服务：{}", serviceFullname);
                                        String[] addressData = serviceDiscovery.discoverService(request.getServiceName(), request.getServiceVersion()).split("/");
                                        node = addressData[0];
                                        serverAddress = addressData[1];
                                    } else {
                                        throw new RuntimeException("服务中心不可用");
                                    }

                                    if (StringUtil.isEmpty(serverAddress)) {
                                        throw new RuntimeException("未查询到服务：" + serviceFullname);
                                    }

                                    logger.debug("选取服务{}节点：{}", serviceFullname, node + "/" + serverAddress);

                                    String[] address = serverAddress.split(":");

                                    String host = address[0];
                                    int port = Integer.parseInt(address[1]);

                                    RPCResponse response = new RPCRequestLauncher(host, port).launch(request);
                                    long requestTimeCost = System.currentTimeMillis() - requestStartTime;

                                    if (response == null) {
                                        throw new RuntimeException(String.format("空的服务器响应(请求号为%s)", request.getRequestId()));
                                    }

                                    logger.debug("请求{}耗时：{}ms", request.getRequestId(), requestTimeCost);

                                    if (response.getException() != null) {
                                        throw response.getException();
                                    } else {
                                        return response.getResult();
                                    }
                                }
                            }
                    );

                    cachedProxy.put(serviceFullname, proxy);
                }
            }
        }
        return (T) proxy;
    }

}
