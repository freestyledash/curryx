package com.nov21th.curry.balance.impl;

import com.nov21th.curry.balance.Balancer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 轮询负载均衡器，将请求按序轮流发送到每台服务器上
 * 考虑在提供服务的多台服务器具有等同的性能的情况下，采用轮询策略是可行的
 * <p>
 * 在千数量级的并发下，所有请求获得一个地址的时间越在20～60ms不等，性能可接受
 *
 * @author 郭永辉
 * @since 1.0 2017/4/4.
 */
public class RoundRobinBalancer implements Balancer {

    private volatile Map<String, Integer> requestMap = new HashMap<String, Integer>();

    public synchronized String elect(String serviceFullname, List<String> candidates) {
        if (!requestMap.containsKey(serviceFullname)) {
            requestMap.put(serviceFullname, 0);
        }

        int index = requestMap.get(serviceFullname) % candidates.size();
        requestMap.put(serviceFullname, index + 1);

        return candidates.get(index);
    }

}

