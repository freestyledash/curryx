package com.freestyledash.curryx.registry;

/**
 * 服务端向注册中心注册服务的接口
 *
 * @author 郭永辉
 * @since 1.0 2017/4/3.
 */
public interface ServiceRegistry {

    /**
     * 向注册中心注册服务
     *
     * @param name          服务名称
     * @param version       服务版本
     * @param serverAddress 提供服务的服务器的地址
     */
    void registerService(String name, String version, String serverAddress);

    /**
     * 向注册中心注册服务
     *
     * @param serviceFullname 服务全称
     * @param serverAddress   提供服务的服务器的地址
     */
    void registerService(String serviceFullname, String serverAddress);

}
