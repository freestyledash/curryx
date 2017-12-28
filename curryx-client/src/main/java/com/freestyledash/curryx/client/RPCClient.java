package com.freestyledash.curryx.client;

import com.freestyledash.curryx.client.handler.RPCRequestLauncher;
import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.common.util.StringUtil;
import com.freestyledash.curryx.discovery.ServiceDiscovery;
import com.freestyledash.curryx.discovery.util.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * rpc通信客户端
 */
public final class RPCClient {

    private static final Logger logger = LoggerFactory.getLogger(RPCClient.class);

    /**
     * 发现服务
     */
    private ServiceDiscovery serviceDiscovery;

    /**
     * 服务代理对象缓存
     */
    private Map<String, Object> cachedProxy;

    /**
     * 通讯服务器客户端
     */
    private RPCRequestLauncher launcher;


    public RPCClient(ServiceDiscovery serviceDiscovery, RPCRequestLauncher launcher) {
        this.serviceDiscovery = serviceDiscovery;
        this.launcher = launcher;
        cachedProxy = new HashMap();
    }

    /**
     * 使用jdk代理生成代理对象，并缓存，对象作用:
     * 1)通过服务名字获得可提供服务的服务器的地址
     * 2)使用netty向服务器发送请求获得相应
     *
     * @param clazz   请求服务的类接口
     * @param version 请求服务版本
     * @param <T>     请求服务的类型
     * @return proxy
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<T> clazz, final String version) {
        //获得需要的服务的全名
        final String serviceFullName = clazz.getName() + Constants.SERVICE_SEP + version;
        Object proxy;
        //这里用双重校验锁保证对于每个serviceFullName内存中有唯一的代理实例与之对应
        if ((proxy = cachedProxy.get(serviceFullName)) == null) {
            synchronized (this) {
                if ((proxy = cachedProxy.get(serviceFullName)) == null) {
                    proxy = Proxy.newProxyInstance(
                            clazz.getClassLoader(),
                            new Class<?>[]{clazz},
                            new RpcInvocationHandler(version, serviceFullName, clazz, serviceDiscovery)
                    );
                    cachedProxy.put(serviceFullName, proxy);
                }
            }
        }
        return (T) proxy;
    }

    /**
     * rpc代理执行类
     */
    public class RpcInvocationHandler implements InvocationHandler {

        /**
         * @param version          服务版本
         * @param serviceFullName  服务全称
         * @param clazz            被代理对象
         * @param serviceDiscovery 服务发现
         */
        public RpcInvocationHandler(String version, String serviceFullName, Class clazz, ServiceDiscovery serviceDiscovery) {
            this.version = version;
            this.serviceFullName = serviceFullName;
            this.clazz = clazz;
            this.serviceDiscovery = serviceDiscovery;
        }

        //版本
        private String version;
        //服务全称
        private String serviceFullName;
        //被代理的class类型
        private Class clazz;
        //服务发现接口
        private ServiceDiscovery serviceDiscovery;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long requestStartTime = System.currentTimeMillis();
            //构建请求对象，使用UUID给每个请求编上id
            RPCRequest request = new RPCRequest();
            request.setServiceName(clazz.getName());
            request.setServiceVersion(version);
            request.setMethodName(method.getName());
            request.setArgsTypes(method.getParameterTypes());
            request.setArgsValues(args);
            request.setRequestId(UUID.randomUUID().toString());
            //请求参数被封装在一个Object数组中，在反序列化的过程中，数组中不为null的元素会被提前
            if (args != null && args.length > 0) {
                boolean[] nonNull = new boolean[args.length];
                for (int i = 0; i < args.length; i++) {
                    nonNull[i] = args[i] != null;
                }
                request.setNonNullArgs(nonNull);
            }
            //名字服务器存放该服务的节点名称
            String node;
            //节点地址
            String serverAddress;
            if (serviceDiscovery != null) {
                logger.debug("向服务中心查询服务：{}", serviceFullName);
                String[] addressData = serviceDiscovery.discoverService(request.getServiceName(), request.getServiceVersion()).split("/");
                node = addressData[0];
                serverAddress = addressData[1];
            } else {
                throw new IllegalAccessException("服务中心不可用");
            }
            if (StringUtil.isEmpty(serverAddress)) {
                throw new IllegalAccessException("未查询到服务：" + serviceFullName);
            }
            logger.debug("选取服务{}节点：{}", serviceFullName, node + "/" + serverAddress);
            String[] address = serverAddress.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);
            RPCResponse response = launcher.launch(host, port, request);
            long requestTimeCost = System.currentTimeMillis() - requestStartTime;
            if (response == null) {
                throw new IllegalAccessException(String.format("空的服务器响应(请求号为%s)", request.getRequestId()));
            }
            logger.debug("请求{}耗时：{}ms", request.getRequestId(), requestTimeCost);

            if (response.getException() != null) {
                throw response.getException();
            } else {
                return response.getResult();
            }
        }
    }
}
