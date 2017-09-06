package com.freestyledash.curryx.registry.constant;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/3.
 */
public abstract class Constants {

    private Constants() {

    }

    /**
     * ZooKeeper默认会话超时时间
     */
    public static final int DEFAULT_ZK_SESSION_TIMEOUT = 5 * 1000;

    /**
     * ZooKeeper默认连接超时时间
     */
    public static final int DEFAULT_ZK_CONNECTION_TIMEOUT = 3 * 1000;

    /**
     * 服务名与版本号的连接符
     */
    public static final String SERVICE_SEP = "-";

}
