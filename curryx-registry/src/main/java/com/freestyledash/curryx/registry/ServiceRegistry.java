package com.freestyledash.curryx.registry;

import com.freestyledash.curryx.server.server.Server;
import com.freestyledash.curryx.serviceContainer.ServiceContainer;

/**
 * 服务端向注册中心注册服务的接口
 *
 * @author zhangyanqi
 */
public interface ServiceRegistry {

    /**
     * 连接注册中心
     */
    void connect();

    /**
     * 服务注册组件需要有服务提供者
     *
     * @param container
     */
    void setServiceContainer(ServiceContainer container);

    /**
     * 关闭服务注册
     */
    void shutdown();

    /**
     * 注册所有服务
     *
     * @return 注册是否成功
     */
    boolean registerAllService();


    /**
     * @param server 通讯服务器
     */
    void setServer(Server server);


    /**
     * @param name 服务器名字
     */
    void setServerName(String name);

}
