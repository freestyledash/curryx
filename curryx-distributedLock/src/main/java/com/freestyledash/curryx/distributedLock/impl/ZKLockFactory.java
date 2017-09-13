package com.freestyledash.curryx.distributedLock.impl;

import com.freestyledash.curryx.distributedLock.Lock;
import com.freestyledash.curryx.distributedLock.LockFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 使用zookeeper实现的lock
 * <p>
 * 设计方案:
 * 初始时在zookeeper下创建/locks节点
 * 锁的资源做为该节点的子节点,子节点名字为资源名字，内容为锁该资源的uuid,只有持有该uuid才能删除该锁
 *
 * @author zhangyanqi
 * @since 1.0 2017/9/13
 */
public class ZKLockFactory implements LockFactory {

    private static ZKLockFactory factory = null;

    public static LockFactory getLockFactory(List<String> zkAddr) {
        synchronized (ZKLockFactory.class) {
            if (factory == null) {
                factory = new ZKLockFactory(zkAddr);
            }
        }
        return factory;
    }

    private static final String ROOT = "/locks"; //存放锁资源的根路径

    private static final Logger logger = LoggerFactory.getLogger(ZKLockFactory.class);

    private CuratorFramework client;

    private static final String FORWARDSLASH = "/";

    public ZKLockFactory(List<String> zkAddr) {
        logger.info("开始初始化客户端");
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
                        5000, //会话超时时间，单位毫秒，默认60000ms
                        3000,//连接创建超时时间，单位毫秒，默认60000ms
                        retryPolicy //重试策略,内建有四种重试策略,也可以自行实现RetryPolicy接口
                );
        logger.info("初始化客户端成功,开始启动");
        client.start();
        logger.info("启动完成,耗时{}", System.currentTimeMillis() - begin);
        //确保存在/locks节点
        init();
    }


    /**
     * 在zookeeper连接建立之后初始化锁节点
     */
    private synchronized void init() {
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
    public Lock getLock(String resourceName) {
        init(); //初始化

        final Lock lock_ = null;
        Lock lock = new Lock();
        String uuid = UUID.randomUUID().toString();
        lock.setUuid(uuid);

        //询问是否有锁
        Stat stat = null;
        try {
            stat = client.checkExists().forPath(ROOT + FORWARDSLASH + resourceName);
        } catch (Exception e) {
            logger.error("创建锁失败");
            throw new IllegalStateException("创建锁失败");
        }

        if (stat == null) { //该资源没有锁
            try {
                client.create().withMode(CreateMode.EPHEMERAL).forPath(ROOT + FORWARDSLASH + resourceName, uuid.getBytes());
            } catch (Exception e) {
                logger.error("创建锁失败");
                throw new IllegalStateException("创建锁失败");
            }
        } else { //该资源有锁
            byte[] bytes = null;
            try {
                bytes = client.getData().storingStatIn(stat).forPath(ROOT + FORWARDSLASH + resourceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String uuid_ = new String(bytes);
            if (uuid_ != null && uuid.equals(uuid_)) { //锁重入
                return lock;
            } else {
                //阻塞,监听锁是否释放,如果释放就尝试获得锁
                /**
                 * 监听数据节点的变化情况
                 */
                final NodeCache nodeCache = new NodeCache(client, ROOT + FORWARDSLASH + resourceName, false);
                try {
                    nodeCache.start(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                nodeCache.getListenable().addListener(
                        () -> {
                            getLock(resourceName);
                        }
                );
            }
            client.clearWatcherReferences(null);
        }
        return lock;
    }

    /**
     * 不停询问锁,在一定次数以内返回
     *
     * @param resourceName 资源名称
     * @param tryTime      请求的次数
     * @param interval     相邻2次请求之间的时间间隔
     * @return 是否成功获得锁
     */
    @Override
    public Lock tryLock(String resourceName, Integer tryTime, Long interval) {
        return null;
    }

    /**
     * 释放锁
     *
     * @return 释放锁是否成功
     */
    @Override
    public boolean unLock(Lock lock) {
        return false;
    }


    public static void main(String[] args) {

        String addr = "127.0.0.1:2181";
        List<String> addrs = new ArrayList<>();
        addrs.add(addr);
        ZKLockFactory zkLock = new ZKLockFactory(addrs);
    }
}
