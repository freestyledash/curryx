package com.freestyledash.curryx.distributedlock;

/**
 * lock接口
 * 一些资源某个时刻只能有一个访问者，那么就提供锁接口，每次都先获得锁之后才能继续访问,访问之后需要释放锁
 *
 * @author zhangyanqi
 * @since 1.0 2017/9/13
 */
public interface LockFactory {

    /**
     * 获得锁,阻塞
     *
     * @param resourceName 资源名字
     * @return 是否成功获得锁
     * @throws Exception 获得锁失败抛出异常
     */
    boolean getLock(String resourceName) throws Exception;

    /**
     * 立刻返回
     *
     * @param resourceName 资源名称
     * @param time         等待时间
     * @return 是否成功获得锁
     * @throws Exception 获得锁失败抛出异常
     */
    boolean tryLock(String resourceName, Integer time) throws Exception;

    /**
     * 释放锁
     *
     * @param resourceName 资源名字
     * @return 释放锁是否成功
     */
    boolean unLock(String resourceName);

}
