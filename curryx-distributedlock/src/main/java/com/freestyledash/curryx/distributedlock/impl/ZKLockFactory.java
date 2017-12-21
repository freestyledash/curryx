package com.freestyledash.curryx.distributedlock.impl;

import com.freestyledash.curryx.distributedlock.LockFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * 使用zookeeper实现的lock
 * <p>
 * 设计方案:
 * 初始时在zookeeper下创建/locks节点
 * 锁的资源做为该节点的子节点,子节点名字为资源名字，内容为锁该线程的uuid,只有持有该uuid才能删除该锁,同时锁也包含重入功能
 *
 * @author zhangyanqi
 * @since 1.0 2017/9/13
 */
public class ZKLockFactory implements LockFactory {


    /**
     * zookeeper客户端工作id
     */
    private ThreadLocal<String> workId = new ThreadLocal();

    /**
     * zookeeper工厂
     */
    private static ZKLockFactory factory = null;

    /**
     * zookeeper地址
     */
    private static List<String> zkAddr;

    private static boolean isInit = false;

    /**
     * 初始化地址
     *
     * @param list
     */
    public static void initConfiguration(List<String> list) {
        //确保只初始化一次
        if (isInit) {
            throw new IllegalStateException("zookeeper地址已经初始化了");
        }
        zkAddr = list;
        isInit = true;
    }

    public static LockFactory getLockFactory() {
        if (zkAddr == null || zkAddr.isEmpty()) {
            throw new IllegalStateException("zookeeper地址未初始化");
        }
        synchronized (ZKLockFactory.class) {
            if (factory == null) {
                factory = new ZKLockFactory(zkAddr);
            }
        }
        return factory;
    }

    /**
     * 存放锁资源的根路径
     */
    private static final String ROOT = "/locks";

    private static final Logger logger = LoggerFactory.getLogger(ZKLockFactory.class);

    private volatile CuratorFramework client;

    private static final String FORWARDSLASH = "/";

    public ZKLockFactory(List<String> zkAddr) {
        long begin = System.currentTimeMillis();
        /*
        连接zookeeper
        确保locks节点存在,该节点为持久节点
        */
        if (zkAddr == null || zkAddr.isEmpty()) {
            throw new IllegalArgumentException("zookeeper地址不能为空");
        }
        StringBuilder sb = new StringBuilder();
        for (String addr : zkAddr) {
            sb.append(addr);
        }
        String addr = sb.toString();
        logger.info("zookeeper地址为{}", addr);
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.client =
                CuratorFrameworkFactory.newClient(
                        addr,
                        //会话超时时间，单位毫秒，默认60000ms
                        5000,
                        //连接创建超时时间，单位毫秒，默认60000ms
                        3000,
                        //重试策略,内建有四种重试策略,也可以自行实现RetryPolicy接口
                        retryPolicy
                );
        logger.info("初始化客户端成功,开始启动");
        client.start();
        logger.info("启动完成,耗时{}", System.currentTimeMillis() - begin);
        //确保存在/locks节点
        initZookeeper();
    }


    /**
     * 在zookeeper连接建立之后初始化锁节点
     */
    private synchronized void initZookeeper() {
        Stat locks = null;
        try {
            locks = client.checkExists().forPath(ROOT);
            if (locks == null) {
                //节点不存在，创建节点
                client.create().withMode(CreateMode.PERSISTENT).forPath(ROOT);
            }
        } catch (Exception e) {
            logger.error("出错了{}", e.getMessage());
        }
    }

    /**
     * 获得锁,阻塞
     *
     * @param resourceName 资源名字
     * @return 是否成功获得锁
     */
    @Override
    public synchronized boolean tryLock(String resourceName, Integer time) throws Exception {
        boolean result = true;
        //尝试的次数够了
        if (time == null || time.equals(0)) {
            return false;
        }
        //初始化
        initZookeeper();
        //询问是否有锁
        Stat stat = null;
        try {
            stat = client.checkExists().forPath(ROOT + FORWARDSLASH + resourceName);
        } catch (Exception e) {
            logger.error("获得锁情况失败");
            throw new IllegalStateException("获得锁情况失败");
        }
        //该资源没有锁
        if (stat == null) {
            if (this.workId.get() == null || this.workId.get().isEmpty()) {
                workId.set(UUID.randomUUID().toString());
            }
            try {
                client.create().withMode(CreateMode.EPHEMERAL).forPath(ROOT + FORWARDSLASH + resourceName, workId.get().getBytes());
            } catch (Exception e) {
                logger.error("创建锁失败");
                throw new IllegalStateException("创建锁失败");
            }
            System.out.println("创建锁成功");
            return true;
        } else { //该资源有锁
            byte[] bytes = null;
            try {
                bytes = client.getData().storingStatIn(stat).forPath(ROOT + FORWARDSLASH + resourceName);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            String workId2 = new String(bytes);
            //锁重入
            if (workId2 != null && this.workId.get() != null && this.workId.get().equals(workId2)) {
                return true;
            } else { //无法获得锁,尝试重新获得
                try {
                    Thread.sleep(300);
                    result = tryLock(resourceName, --time);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    /**
     * 阻塞
     *
     * @param resourceName 资源名称
     * @return 是否成功获得锁
     */
    @Override
    public synchronized boolean getLock(String resourceName) throws Exception {
        return tryLock(resourceName, 5);
    }

    /**
     * 释放锁
     *
     * @return 释放锁是否成功
     */
    @Override
    public synchronized boolean unLock(String resourceName) {
        //询问是否有锁
        Stat stat = null;
        try {
            stat = client.checkExists().forPath(ROOT + FORWARDSLASH + resourceName);
        } catch (Exception e) {
            logger.error("获得锁情况失败");
            throw new IllegalStateException("获得锁情况失败");
        }
        if (stat == null) {
            // 改资源没有锁
            return true;
        } else {
            byte[] bytes = null;
            try {
                bytes = client.getData().storingStatIn(stat).forPath(ROOT + FORWARDSLASH + resourceName);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
            String workId2 = new String(bytes);
            //可以解锁
            String s = workId.get();
            boolean b = s != null && (workId2 == null || s.equals(workId2) || workId2.isEmpty());
            if (b) {
                workId.remove();
                try {
                    client.delete().forPath(ROOT + FORWARDSLASH + resourceName);
                    return true;
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            } else { //改资源上锁,但是不是该client上的锁
                return false;
            }
        }
    }
}
