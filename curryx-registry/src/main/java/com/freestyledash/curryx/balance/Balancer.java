package com.freestyledash.curryx.balance;

import java.util.List;

/**
 * 客户端请求的可用服务器的负载均衡接口
 *
 * @author 郭永辉
 * @since 1.0 2017/4/4.
 */
public interface Balancer {

    /**
     * 从候选地址<code>candidates</code>中根据一定的负载均衡算法选出一台服务器的地址
     *
     * @param serviceFullname 服务的全称
     * @param candidates      服务的候选地址
     * @return 选中的服务器的地址
     */
    String elect(String serviceFullname, List<String> candidates);

}
