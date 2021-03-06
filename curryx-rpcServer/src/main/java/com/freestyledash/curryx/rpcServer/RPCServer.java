package com.freestyledash.curryx.rpcServer;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.server.server.Server;
import com.freestyledash.curryx.serviceContainer.ServiceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * RPC服务端
 * 职责：
 * 加载服务
 * 启动服务
 * 注册服务
 * <p>
 * 实现ApplicationContextAware
 * 在该类被spring初始化后会执行setApplicationContext方法
 */
public class RPCServer {


    private static final Logger LOGGER = LoggerFactory.getLogger(RPCServer.class);

    /**
     * 服务加载器
     */
    private ServiceContainer serviceContainer;

    /**
     * 服务器名字，在注册服务的时候，服务节点名字为该名字，默认为{ip}:{一个随机的UUID},不推荐使用
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


    private void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.shutdown();
        }, "rpcServerShutDownHook"));
        serviceContainer.load();
        this.server.setServiceContainer(serviceContainer);
        this.serviceRegistry.setServiceContainer(serviceContainer);
        this.serviceRegistry.setServerName(this.serverName);
        this.serviceRegistry.setServer(server);
    }

    public RPCServer(ServiceContainer loader, ServiceRegistry serviceRegistry, Server server) {
        this(loader, serviceRegistry, server, UUID.randomUUID().toString());
    }

    public RPCServer(ServiceContainer loader, ServiceRegistry serviceRegistry, Server server, String serverName) {
        this.serviceContainer = loader;
        this.serviceRegistry = serviceRegistry;
        this.server = server;
        this.serverName = serverName;
        if(serverName.contains("#")){
            throw  new IllegalArgumentException("serverName不能包含#");
        }
        this.serverAddress = server.getAddress();
    }

    /**
     * 在新线程中启动netty
     * 将服务注册到名字服务器中
     * 只有服务启动了，才能注册服务到名字服务器中
     */
    public void start() {
        init();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(
                () -> this.server.start(countDownLatch)).start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("启动netty失败");
            return;
        }
        serviceRegistry.connect();
        serviceRegistry.registerAllService();
    }

    /**
     * 停止服务器
     */
    public void shutdown() {
        LOGGER.info("服务关闭中...");
        this.serviceRegistry.shutdown();
        this.server.shutdown();
        LOGGER.info("服务关闭完成");
    }
}
