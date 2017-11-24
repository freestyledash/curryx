package com.freestyledash.curryx.server.server;

import java.util.concurrent.CountDownLatch;

/**
 * 通讯服务器类
 * 负责服务器的启动
 * 服务器的关闭
 * @author zhangyanqi
 * @since 1.0 2017/11/23
 */
public interface Server {

    /**
     * 服务开启
     * @param latch  服务需要在某个时间点之后开启
     */
    void start(CountDownLatch latch);

    /**
     * 服务关闭
     */
    void shutdown();

}
