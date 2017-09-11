package com.freestyledash.curryx.client;

import com.freestyledash.curryx.client.handler.RPCRequestLauncher;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.common.util.StringUtil;
import com.freestyledash.curryx.registry.ServiceDiscovery;
import com.freestyledash.curryx.registry.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * rpc通信客户端
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
        cachedProxy = new HashMap();
    }

    /**
     * 使用jdk代理生成代理对象，并缓存，对象作用:
     * 1)通过服务名字获得可提供服务的服务器的地址
     * 2)使用netty向服务器发送请求获得相应
     *
     * @param clazz 请求服务的类接口
     * @param version 请求服务版本
     * @param <T> 请求服务的类型
     * @return proxy
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<T> clazz, final String version) {
        final String serviceFullName = clazz.getName() + Constants.SERVICE_SEP + version; //获得需要的服务的全名
        Object proxy;
        //这里用双重校验锁保证对于每个serviceFullname内存中有唯一的代理实例与之对应
        if ((proxy = cachedProxy.get(serviceFullName)) == null) {
            synchronized (this) {
                if ((proxy = cachedProxy.get(serviceFullName)) == null) {
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

                                    if (args != null && args.length > 0) {
                                        boolean[] nonNull = new boolean[args.length];
                                        for (int i = 0; i < args.length; i++) {
                                            nonNull[i] =  args[i] != null;
                                        }
                                        request.setNonNullArgs(nonNull);
                                    }

                                    String node;
                                    String serverAddress;
                                    if (serviceDiscovery != null) {
                                        logger.debug("向服务中心查询服务：{}", serviceFullName);
                                        String[] addressData = serviceDiscovery.discoverService(request.getServiceName(), request.getServiceVersion()).split("/");
                                        node = addressData[0];
                                        serverAddress = addressData[1];
                                    } else {
                                        throw new RuntimeException("服务中心不可用");
                                    }

                                    if (StringUtil.isEmpty(serverAddress)) {
                                        throw new RuntimeException("未查询到服务：" + serviceFullName);
                                    }

                                    logger.debug("选取服务{}节点：{}", serviceFullName, node + "/" + serverAddress);

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
                    cachedProxy.put(serviceFullName, proxy);
                }
            }
        }
        return (T) proxy;
    }
}
