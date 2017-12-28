package com.freestyledash.curryx.discovery.util.balance.impl;

import com.freestyledash.curryx.discovery.util.balance.Balancer;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡算法
 *
 * @author zhangyanqi
 * @since 1.0 2017/9/11
 */
public class RandomBalancer implements Balancer {

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    /**
     * 从候选地址<code>candidates</code>中根据一定的负载均衡算法选出一台服务器的地址
     *
     * @param serviceFullName 服务的全称
     * @param candidates      服务的候选地址
     * @return 选中的服务器的地址
     */
    @Override
    public String elect(String serviceFullName, List<String> candidates) {
        int size = candidates.size();
        if (size == 0) {
            return null;
        }
        int i = random.nextInt(size);
        return candidates.get(i);
    }
}