package com.freestyledash.curryx.server;

import com.freestyledash.curryx.registry.Constants;
import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.server.annotation.Service;
import com.freestyledash.curryx.server.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * RPC服务端
 * 实现ApplicationContextAware
 * 在该类被spring初始化后会执行setApplicationContext方法
 */
public class RPCServer implements ApplicationContextAware {


    private static final Logger LOGGER = LoggerFactory.getLogger(RPCServer.class);

    /**
     * 服务器名字，在注册服务的时候，服务节点名字为该名字，默认为一个随机的UUID,不推荐使用
     */
    private String serverName;

    /**
     * 通讯服务器
     */
    private Server server;

    /**
     * 本机网络进程的地址，格式为ip:port
     */
    private String serverAddress;

    /**
     * 注册服务的接口
     */
    private ServiceRegistry serviceRegistry;

    /**
     * 保存服务容器
     */
    private Map<String, Object> serviceMap = new HashMap();

    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry, Server server) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
        this.server = server;
        this.serverName = UUID.randomUUID().toString();
    }

    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry, Server server, String serverName) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
        this.server = server;
        this.serverName = serverName;
    }

    /**
     * 通过spring扫描获得所有被标记为service的类,并把它们放入serviceMap中
     *
     * @param context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        //扫描指定路径下被Service注解修饰的类
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
        //若扫描到的map为空则说明当前服务器没有提供任何服务，警告
        if (map == null || map.size() == 0) {
            LOGGER.warn("在当前服务器下没有任何服务");
            return;
        }
        //对扫描到的每一个service，记录其服务名称和版本
        for (Object serviceBean : map.values()) {
            Service serviceAnnotation = serviceBean.getClass().getAnnotation(Service.class);
            boolean assignableFrom = serviceAnnotation.name().isAssignableFrom(serviceBean.getClass());
            if (!assignableFrom) {
                throw new IllegalStateException(serviceBean.getClass().getName() + " 注解中的接口和该类实现的接口不一致");
            }
            String serviceFullName = serviceAnnotation.name().getName() + Constants.SERVICE_SEP + serviceAnnotation.version();
            serviceMap.put(serviceFullName, serviceBean);
            LOGGER.debug("扫描到服务：{}", serviceFullName);
        }
    }

    /**
     * 在新线程中启动netty
     * 将服务注册到名字服务器中
     * 只有服务启动了，才能注册服务到名字服务器中
     */
    public void start() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(
                () -> this.server.start(countDownLatch)).start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("启动netty失败");
            System.exit(0);
        }
        registerServices();
    }

    /**
     * 注册服务
     */
    private void registerServices() {
        if (serviceRegistry != null) {
            for (String serviceFullName : serviceMap.keySet()) {
                LOGGER.debug("向注册中心注册服务：{}", serviceFullName);
                serviceRegistry.registerService(serviceFullName, serverName, serverAddress);
            }
        } else {
            throw new RuntimeException("服务中心不可用");
        }
    }

}
