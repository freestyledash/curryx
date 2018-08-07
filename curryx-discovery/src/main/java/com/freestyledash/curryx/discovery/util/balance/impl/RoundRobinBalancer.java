package com.freestyledash.curryx.discovery.util.balance.impl;

import com.freestyledash.curryx.discovery.util.balance.Balancer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询负载均衡器，将请求按序轮流发送到每台服务器上
 * 考虑在提供服务的多台服务器具有等同的性能的情况下，采用轮询策略是可行的
 * 在千数量级的并发下，所有请求获得一个地址的时间越在20～60ms不等，性能可接受
 *
 * @author zhangyanqi
 */
public class RoundRobinBalancer implements Balancer {

    /**
     * 记录每个服务使用的服务历史
     */
    private Map<String, AtomicLong> requestMap = new ConcurrentHashMap<>();

    /**
     * 从候选list中选出合适的节点
     * 此时会出现线程安全问题，但是不会对业务造成影响
     *
     * @param serviceFullName 服务的全称
     * @param candidates      服务的候选地址
     * @return 选择的节点
     */
    @Override
    public String elect(String serviceFullName, List<String> candidates) {
        requestMap.putIfAbsent(serviceFullName, new AtomicLong(0));
        AtomicLong atomicLong = requestMap.get(serviceFullName);
        int i = atomicLong.intValue() % candidates.size();
        atomicLong.set(i);
        return candidates.get(i);
    }

}

