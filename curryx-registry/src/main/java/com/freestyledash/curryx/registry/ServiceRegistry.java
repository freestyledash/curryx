package com.freestyledash.curryx.registry;

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

}
