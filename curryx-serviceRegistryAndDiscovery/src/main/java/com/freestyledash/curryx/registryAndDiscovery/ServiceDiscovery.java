package com.freestyledash.curryx.registryAndDiscovery;

/**
 * 客户端从注册中心发现服务
 */
public interface ServiceDiscovery {

    /**
     * 从注册中心发现服务
     *
     * @param name    服务名称
     * @param version 服务版本
     * @return 提供该服务的地址
     */
    String discoverService(String name, String version) throws Exception;

}
