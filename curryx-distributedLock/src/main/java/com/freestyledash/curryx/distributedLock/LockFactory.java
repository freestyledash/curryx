package com.freestyledash.curryx.distributedLock;

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
     */
    Lock getLock(String resourceName);

    /**
     * 不停询问锁,在一定次数以内返回
     *
     * @param resourceName 资源名称
     * @param tryTime      请求的次数
     * @param interval     相邻2次请求之间的时间间隔
     * @return 是否成功获得锁
     */
    Lock tryLock(String resourceName, Integer tryTime, Long interval);

    /**
     * 释放锁
     *
     * @return 释放锁是否成功
     */
    boolean unLock(Lock lock);

}
