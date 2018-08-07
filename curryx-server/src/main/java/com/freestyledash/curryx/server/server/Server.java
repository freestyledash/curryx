package com.freestyledash.curryx.server.server;

import java.util.concurrent.CountDownLatch;

/**
 * 通讯服务器类
 * 负责服务器的启动
 * 服务器的关闭
 *
 * @author zhangyanqi
 * @since 1.0 2017/11/23
 */
public interface Server {

    /**
     * 服务开启
     *
     * @param latch 控制服务启动的时间
     */
    void start(CountDownLatch latch);

    /**
     * 服务关闭
     */
    void shutdown();

}
