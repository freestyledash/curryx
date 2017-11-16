package com.freestyledash.curryx.registry.impl;

import com.freestyledash.curryx.balance.Balancer;
import com.freestyledash.curryx.registry.ServiceDiscovery;
import com.freestyledash.curryx.registry.constant.Constants;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * 使用ZooKeeper实现的服务发现
 */
public class ZooKeeperServiceDiscovery implements ServiceDiscovery, IZkStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    /**
     * 缓存的地址
     */
    private final HashMap<String, List<String>> cachedServiceAddress;

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

    /**
     * @param zkAddress   zookeeper地址
     * @param serviceRoot 根目录
     * @param balancer    负载均衡器
     */
    public ZooKeeperServiceDiscovery(String zkAddress, String serviceRoot, Balancer balancer) {
        this(zkAddress, serviceRoot, balancer, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

    /**
     * @param zkAddress           zookeeper地址
     * @param serviceRoot         根目录
     * @param balancer            负载均衡器
     * @param zkSessionTimeout    zookeeper Session 过期时间
     * @param zkConnectionTimeout zookeeper 连接过期时间
     */
    public ZooKeeperServiceDiscovery(String zkAddress, String serviceRoot, Balancer balancer, int zkSessionTimeout, int zkConnectionTimeout) {
        this.zkAddress = zkAddress;
        this.serviceRoot = serviceRoot;
        this.balancer = balancer;
        this.zkSessionTimeout = zkSessionTimeout;
        this.zkConnectionTimeout = zkConnectionTimeout;

        // ZooKeeper客户端只初始化一次，防止每次发现服务都需要初始化一次ZooKeeper客户端而导致效率低下
        zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
        this.zkClient.subscribeStateChanges(this); //向zkClient注册事件监听器
        cachedServiceAddress = new HashMap(); // 初始化缓存
    }

    /**
     * 向zookeeper发现服务
     *
     * @param name    服务名称
     * @param version 服务版本
     * @return 服务地址
     * @throws Exception
     */
    public String discoverService(String name, String version) throws Exception {
        if (zkAddress.contains(",")) {
            logger.debug("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.debug("连接到ZooKeeper单机服务器：{}", zkAddress);
        }

        String serviceFullName = name + Constants.SERVICE_SEP + version;
        String servicePath = serviceRoot + "/" + serviceFullName;

        //询问缓存是否有服务地址,如果有，使用缓存的地址，并使用负载均衡获一个地址返回
        List<String> childNodes = null;
        synchronized (ZooKeeperServiceDiscovery.class) {
            childNodes = cachedServiceAddress.get(serviceFullName);
        }
        if (childNodes != null) {
            logger.debug("使用缓存,获取到{}服务的{}个可用节点", serviceFullName, childNodes.size());
            String winner = balancer.elect(serviceFullName, childNodes);
            String data = zkClient.readData(servicePath + "/" + winner);
            return winner + "/" + data;
        }

        if (!zkClient.exists(servicePath)) {
            throw new RuntimeException(String.format("服务路径(%s)不存在", servicePath));
        }

        childNodes = zkClient.getChildren(servicePath);
        if (childNodes == null || childNodes.size() == 0) {
            throw new RuntimeException(String.format("服务路径(%s)下无可用服务器节点", servicePath));
        }
        cachedServiceAddress.put(serviceFullName, childNodes); //将内容存入缓存

        logger.debug("获取到{}服务的{}个可用节点,并加入缓存", serviceFullName, childNodes.size());
        String winner = balancer.elect(serviceFullName, childNodes);//读取节点
        String data = zkClient.readData(servicePath + "/" + winner); //读取节点内的内容
        String result = winner + "/" + data;
        return result;
    }

    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            logger.debug("观察到ZooKeeper状态码：{}，清除缓存", state.getIntValue());
            synchronized (ZooKeeperServiceDiscovery.class) {
                cachedServiceAddress.clear();
            }
        }
    }

    @Override
    public void handleNewSession() throws Exception {
        logger.debug("ZooKeeper会话过期，创建新的会话");
    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        logger.debug("ZooKeeper会话过期,创建新的会话,但是失败了");
        synchronized (ZooKeeperServiceDiscovery.class) {
            cachedServiceAddress.clear();
        }
    }


}
