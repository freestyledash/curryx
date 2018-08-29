package com.freestyledash.curryx.serviceContainer;

import java.util.Map;

/**
 * 服务容器
 * 底层维护一个map
 * key: serviceName
 * value: object
 * 负责服务实例加载
 * 服务对象获得
 *
 * @author zhangyanqi
 * @since 1.0 2018/8/29
 */
public interface ServiceContainer {

    /**
     * 加载服务
     *
     * @return 服务加载是否成功
     */
    boolean load();

    /**
     * 返回加载的服务
     *
     * @param serviceName 服务名字
     * @return 实例
     */
    Object get(String serviceName);

    /**
     * 获得加载的所有服务
     *
     * @return
     */
    Map<String,Object> getServiceMap();

}
