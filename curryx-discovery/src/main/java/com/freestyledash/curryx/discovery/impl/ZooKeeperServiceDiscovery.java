package com.freestyledash.curryx.discovery.impl;

/**
 * @author zhangyanqi
 * @since 1.0 2017/11/27
 */

import com.freestyledash.curryx.discovery.ServiceDiscovery;
import com.freestyledash.curryx.discovery.util.balance.Balancer;
import com.freestyledash.curryx.discovery.util.balance.impl.RandomBalancer;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.freestyledash.curryx.discovery.util.constant.Constants.*;


/**
 * 使用ZooKeeper实现服务发现
 */
class ZooKeeperServiceDiscovery implements ServiceDiscovery, IZkStateListener, IZkChildListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    /**
     * 缓存服务名称和对应的地址
     */
    private final Map<String, List<String>> cachedServiceAddress;

    /**
     * ZooKeeper服务器地址（单机模式下格式为ip:port，集群模式下地址之间使用逗号分隔，例如xxx:xx,vvv:xv）
     */
    private final String zkAddress;

    /**
     * 所有服务在ZooKeeper下的根节点
     * <p>
     * 例如，若根节点为/x，则所有服务都托管在ZooKeeper服务器的/x/路径下
     */
    private final String serviceRoot;

    /**
     * 负载均衡策略,如果一个服务有多个提供者，则使用负载均衡获得一个提供者
     */
    private Balancer balancer;

    /**
     * ZooKeeper客户端实例
     */
    private final ZkClient zkClient;

    /**
     * @param zkAddress   zookeeper地址
     * @param serviceRoot 根目录
     * @param balancer    负载均衡器
     */
    public ZooKeeperServiceDiscovery(String zkAddress, String serviceRoot, Balancer balancer) {
        this(zkAddress, serviceRoot, balancer, DEFAULT_ZK_SESSION_TIMEOUT, DEFAULT_ZK_SESSION_TIMEOUT);
    }

    public ZooKeeperServiceDiscovery(String zkAddress, String serviceRoot) {
        this(zkAddress, serviceRoot, new RandomBalancer(), DEFAULT_ZK_SESSION_TIMEOUT, DEFAULT_ZK_SESSION_TIMEOUT);
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
        zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
        //注册事件监听器
        this.zkClient.subscribeStateChanges(this);
        this.zkClient.subscribeChildChanges(serviceRoot, this);
        // 初始化缓存
        cachedServiceAddress = new ConcurrentHashMap<>();
        if (zkAddress.contains(COMMA)) {
            LOGGER.info("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            LOGGER.info("连接到ZooKeeper单机服务器：{}", zkAddress);
        }
    }

    /**
     * 向zookeeper询问服务
     *
     * @param name    服务名称
     * @param version 服务版本
     * @return 服务地址 格式为 服务节点名称/服务节点地址
     * @throws Exception
     */
    @Override
    public String discoverService(String name, String version) throws Exception {
        String serviceFullName = name + SERVICE_SEP + version;
        String servicePath = serviceRoot + "/" + serviceFullName;
        //询问缓存是否有服务地址,如果有，使用缓存的地址，并使用负载均衡获一个地址返回
        List<String> serviceNodes = cachedServiceAddress.get(serviceFullName);
        //cache hit
        if (serviceNodes != null && !serviceNodes.isEmpty()) {
            String winner = balancer.elect(serviceFullName, serviceNodes);
            String[] split = winner.split("#");
            if (split.length < 1) {
                throw new IllegalStateException("错误的服务节点信息");
            }
            LOGGER.info("命中缓存,获取到{}服务的{}个可用节点{}中的的{}节点", serviceFullName, serviceNodes.size(), serviceNodes.toString(), winner);
            return winner + "/" + split[1];
        }
        //cache miss
        if (!zkClient.exists(servicePath)) {
            throw new RuntimeException(String.format("服务路径(%s)不存在", servicePath));
        }
        List<String> childNodes = zkClient.getChildren(servicePath);
        if (childNodes == null || childNodes.size() == 0) {
            throw new RuntimeException(String.format("服务路径(%s)下无可用服务器节点", servicePath));
        }
        serviceNodes = new ArrayList<>(10);
        for (String serverName : childNodes) {
            String data = zkClient.readData(servicePath + "/" + serverName);
            String serviceNode = serverName + "#" + data;
            serviceNodes.add(serviceNode);
        }
        //将内容存入缓存
        cachedServiceAddress.put(serviceFullName, serviceNodes);
        LOGGER.info("获取到{}服务的{}个可用节点{},并加入缓存", serviceFullName, serviceNodes.size(), serviceNodes.toString());
        //注册监听
        zkClient.subscribeChildChanges(servicePath, this);
        //读取节点
        String winner = balancer.elect(serviceFullName, childNodes);
        //读取节点内的内容
        String data = zkClient.readData(servicePath + "/" + winner);
        String result = winner + "/" + data;
        return result;
    }

    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            LOGGER.info("观察到ZooKeeper事件SyncConnected");
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {
            LOGGER.warn("检测到zookeeper事件:Disconnected,清除缓存");
            cachedServiceAddress.clear(); //监听丢失
        }
        if (state == Watcher.Event.KeeperState.Expired) {
            LOGGER.warn("检测到zookeeper事件:Expired,清除缓存");
            cachedServiceAddress.clear();
        }
    }

    @Override
    public void handleNewSession() throws Exception {
        LOGGER.info("ZooKeeper创建新的会话");
    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        LOGGER.error("ZooKeeper会话过期,创建新的会话,但是失败");
    }

    /**
     * 子节点发生改变后通知
     * Called when the children of the given path changed.
     *
     * @param parentPath    The parent path
     * @param currentChilds The children or null if the root node (parent path) was deleted.
     * @throws Exception
     */
    @Override
    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
        //todo 优化缓存

        if (parentPath.equals(serviceRoot)) {
            LOGGER.info("根节点{}的子节点发生变化,清除失效的缓存", parentPath);
            cachedServiceAddress.clear();
        } else {
            LOGGER.info("服务{}的子节点发生变化,清除该服务对应的缓存", parentPath);
            String serviceFullName = parentPath.substring(serviceRoot.length() + 1);
            cachedServiceAddress.remove(serviceFullName);
        }
    }
}