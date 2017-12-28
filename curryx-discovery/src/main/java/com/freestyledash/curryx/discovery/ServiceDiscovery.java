package com.freestyledash.curryx.discovery;

/**
 * 客户端从注册中心发现服务
 *
 * @author zhangyanqi
 */
public interface ServiceDiscovery {

    /**
     * 从注册中心发现服务
     *
     * @param name    服务名称
     * @param version 服务版本
     * @return 提供该服务的地址
     * @throws Exception 名字服务器没有找到或者没有服务时抛出异常
     */
    String discoverService(String name, String version) throws Exception;

}
