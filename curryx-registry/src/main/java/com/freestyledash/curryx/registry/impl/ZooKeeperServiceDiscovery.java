package com.freestyledash.curryx.registry.impl;

import com.freestyledash.curryx.registry.ServiceDiscovery;
import com.freestyledash.curryx.registry.constant.Constants;
import com.freestyledash.curryx.balance.Balancer;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 使用ZooKeeper实现的服务发现
 *
 * @author 郭永辉
 * @since 1.0 2017/4/3.
 */
public class ZooKeeperServiceDiscovery implements ServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    /**
     * ZooKeeper服务器地址（单机模式下格式为ip:port，集群模式下地址之间使用逗号分隔）
     */
    private final String zkAddress;

    /**
     * 所有服务在ZooKeeper下的根节点
     * <p>
     * 例如，若根节点为/com.nov21th，则所有服务都托管在ZooKeeper服务器的/com.nov21th/路径下
     */
    private final String serviceRoot;

    /**
     * 负载均衡策略的接口
     */
    private Balancer balancer;

    /**
     * ZooKeeper客户端会话超时时间
     */
    private final int zkSessionTimeout;

    /**
     * ZooKeeper客户端连接超时时间
     */
    private final int zkConnectionTimeout;

    /**
     * ZooKeeper客户端实例
     */
    private ZkClient zkClient;

    public ZooKeeperServiceDiscovery(String zkAddress, String serviceRoot, Balancer balancer) {
        this(zkAddress, serviceRoot, balancer, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

    public ZooKeeperServiceDiscovery(String zkAddress, String serviceRoot, Balancer balancer, int zkSessionTimeout, int zkConnectionTimeout) {
        this.zkAddress = zkAddress;
        this.serviceRoot = serviceRoot;
        this.balancer = balancer;
        this.zkSessionTimeout = zkSessionTimeout;
        this.zkConnectionTimeout = zkConnectionTimeout;

        // ZooKeeper客户端只初始化一次，防止每次发现服务都需要初始化一次ZooKeeper客户端而导致效率低下
        zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
    }

    public String discoverService(String name, String version) throws Exception {
        if (zkAddress.contains(",")) {
            logger.debug("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.debug("连接到ZooKeeper单机服务器：{}", zkAddress);
        }

        String serviceFullname = name + Constants.SERVICE_SEP + version;
        String servicePath = serviceRoot + "/" + serviceFullname;

        if (!zkClient.exists(servicePath)) {
            throw new RuntimeException(String.format("服务路径(%s)不存在", servicePath));
        }

        List<String> childNodes = zkClient.getChildren(servicePath);
        if (childNodes == null || childNodes.size() == 0) {
            throw new RuntimeException(String.format("服务路径(%s)下无可用服务器节点", servicePath));
        }

        String winner = balancer.elect(serviceFullname, childNodes);

        logger.debug("获取到{}服务的{}个可用节点", serviceFullname, childNodes.size());

        return winner + "/" + zkClient.readData(servicePath + "/" + winner);
    }
}
