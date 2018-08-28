package com.freestyledash.curryx.registry.util;

/**
 * zookeeper使用的常量
 *
 * @author zhangyanqi
 */
public final class Constants {

    private Constants() {

    }

    /**
     * ZooKeeper默认会话超时时间，单位毫秒
     */
    public static final int DEFAULT_ZK_SESSION_TIMEOUT = 5 * 1000;

    /**
     * ZooKeeper默认连接超时时间，单位毫秒
     */
    public static final int DEFAULT_ZK_CONNECTION_TIMEOUT = 3 * 1000;


}
