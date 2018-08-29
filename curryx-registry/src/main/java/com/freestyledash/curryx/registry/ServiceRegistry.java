package com.freestyledash.curryx.registry;

import com.freestyledash.curryx.server.server.Server;

/**
 * 服务端向注册中心注册服务的接口
 *
 * @author zhangyanqi
 */
public interface ServiceRegistry {

    /**
     * 向注册中心注册服务
     *
     * @param serviceFullName 服务全称
     * @param serverName      服务提供者的名字
     * @param serverAddress   提供服务的服务器的地址
     */
    void registerService(String serviceFullName, String serverName, String serverAddress);


    /**
     * 注册组件需要能够检测到服务器的健康状态
     *
     * @param server
     */
    void setServer(Server server);


    /**
     * 关闭服务注册
     */
    void shutdown();

}
