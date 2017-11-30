package com.freestyledash.curryx.registryAndDiscovery.discovery.impl;

/**
 * @author zhangyanqi
 * @since 1.0 2017/11/27
 */

import com.freestyledash.curryx.registryAndDiscovery.discovery.ServiceDiscovery;
import com.freestyledash.curryx.registryAndDiscovery.util.balance.Balancer;
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

import static com.freestyledash.curryx.registryAndDiscovery.util.constant.Constants.DEFAULT_ZK_SESSION_TIMEOUT;
import static com.freestyledash.curryx.registryAndDiscovery.util.constant.Constants.SERVICE_SEP;

/**
 * 使用ZooKeeper实现的服务发现
 */
class ZooKeeperServiceDiscovery implements ServiceDiscovery, IZkStateListener, IZkChildListener {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    /**
     * 锁对象
     */
    private final Object lock = new Object();

    /**
     * 记录被监听的node
     */
    private final List listenedNodeList;

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
        this.zkClient.subscribeStateChanges(this); //注册事件监听器
        this.zkClient.subscribeChildChanges(serviceRoot, this);
        cachedServiceAddress = new ConcurrentHashMap<>(); // 初始化缓存
        listenedNodeList = new ArrayList();
    }

    /**
     * 向zookeeper询问服务
     *
     * @param name    服务名称
     * @param version 服务版本
     * @return 服务地址 格式为 服务节点名称/服务节点地址
     * @throws Exception
     */
    public String discoverService(String name, String version) throws Exception {
        if (zkAddress.contains(",")) {
            logger.info("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.info("连接到ZooKeeper单机服务器：{}", zkAddress);
        }
        String serviceFullName = name + SERVICE_SEP + version;
        String servicePath = serviceRoot + "/" + serviceFullName;
        //询问缓存是否有服务地址,如果有，使用缓存的地址，并使用负载均衡获一个地址返回
        List<String> childNodes = cachedServiceAddress.get(serviceFullName);
        if (childNodes != null && !childNodes.isEmpty()) {
            logger.info("使用缓存,获取到{}服务的{}个可用节点", serviceFullName, childNodes.size());
            String winner = balancer.elect(serviceFullName, childNodes);
            String data = zkClient.readData(servicePath + "/" + winner);
            return winner + "/" + data;
        }
        //没有使用缓存
        if (!zkClient.exists(servicePath)) {
            throw new RuntimeException(String.format("服务路径(%s)不存在", servicePath));
        }
        childNodes = zkClient.getChildren(servicePath);
        if (childNodes == null || childNodes.size() == 0) {
            throw new RuntimeException(String.format("服务路径(%s)下无可用服务器节点", servicePath));
        }
        //将内容存入缓存
        cachedServiceAddress.put(serviceFullName, childNodes);
        logger.info("获取到{}服务的{}个可用节点,并加入缓存", serviceFullName, childNodes.size());
        //将这个节点加入被监听的行列中,如果node
        if (!listenedNodeList.contains(serviceFullName)) {//如果没有被监听
            synchronized (lock) {
                if (!listenedNodeList.contains(serviceFullName)) {
                    this.zkClient.subscribeChildChanges(servicePath, this);
                    this.listenedNodeList.add(serviceFullName);
                    logger.info("将{}节点加入子节点监听范围中", servicePath);
                }
            }
        }
        String winner = balancer.elect(serviceFullName, childNodes);//读取节点
        String data = zkClient.readData(servicePath + "/" + winner); //读取节点内的内容
        String result = winner + "/" + data;
        return result;
    }

    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            logger.info("观察到ZooKeeper状态SyncConnected,清除缓存");
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {
            logger.warn("检测到zookeeper事件:Disconnected(断开连接),清除缓存");
            cachedServiceAddress.clear();
        }
        if (state == Watcher.Event.KeeperState.Expired) {
            logger.warn("检测到zookeeper事件:Expired(session过期)");
        }
    }

    @Override
    public void handleNewSession() throws Exception {
        logger.info("ZooKeeper创建新的会话,但是不清清除缓存");
    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        logger.error("ZooKeeper会话过期,创建新的会话,但是失败了");
        cachedServiceAddress.clear();
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
        if (parentPath.equals(serviceRoot)) {//如果是根节点改变,则删除所有缓存
            logger.info("根节点{}的子节点发生变化,清除所有缓存", parentPath);
            cachedServiceAddress.clear();
        } else {
            logger.info("服务{}的子节点发生变化,清除该服务对应的缓存", parentPath);
            String serviceFullName = parentPath.substring(serviceRoot.length());
            cachedServiceAddress.remove(serviceFullName);
        }
    }

}