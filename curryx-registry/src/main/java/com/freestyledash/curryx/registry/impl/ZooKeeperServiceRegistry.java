package com.freestyledash.curryx.registry.impl;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.registry.util.Constants;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.freestyledash.curryx.registry.util.Constants.COMMA;

/**
 * 使用ZooKeeper名字服务器实现的服务发现
 *
 * @author zhangyanqi
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry, IZkStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    /**
     * ZooKeeper客户端实例
     */
    private final ZkClient zkClient;

    /**
     * 所有服务在ZooKeeper下的根节点
     */
    private final String serviceRoot;

    /**
     * 储存已经或者曾经被发布到zookeeper中的服务名称和服务地址
     */
    private Map<String, ServiceNode> registeredServiceMapCache;

    /**
     * @param zkAddress   zookeeper地址
     * @param serviceRoot 根节点
     */
    public ZooKeeperServiceRegistry(String zkAddress, String serviceRoot) {
        this(zkAddress, serviceRoot, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

    /**
     * @param zkAddress           zookeeper地址
     * @param serviceRoot         根节点
     * @param zkSessionTimeout    session过期时间
     * @param zkConnectionTimeout 连接失效时间
     */
    public ZooKeeperServiceRegistry(String zkAddress, String serviceRoot, int zkSessionTimeout, int zkConnectionTimeout) {
        if (zkAddress == null || "".equals(zkAddress)) {
            throw new RuntimeException("无效的ZooKeeper地址");
        }
        if (serviceRoot == null || "".equals(serviceRoot)) {
            throw new RuntimeException("无效的服务根节点");
        }
        this.registeredServiceMapCache = new HashMap<>(20);
        this.serviceRoot = serviceRoot;
        this.zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
        if (zkAddress.contains(COMMA)) {
            LOGGER.info("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            LOGGER.info("连接到ZooKeeper单机服务器：{}", zkAddress);
        }
        this.zkClient.subscribeStateChanges(this);
        if (!zkClient.exists(serviceRoot)) {
            zkClient.createPersistent(serviceRoot);
        }
        LOGGER.info("服务根节点（持久节点）：{}", serviceRoot);
    }

    /**
     * 注册服务
     *
     * @param serviceFullName 服务全称
     * @param serviceName     服务提供者的名字
     * @param serviceAddress  提供服务的服务器的地址
     */
    @Override
    public void registerService(String serviceFullName, String serviceName, String serviceAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceRoot);
        sb.append('/');
        sb.append(serviceFullName);
        String servicePath = sb.toString();
        //如果root/serviceName不存在，则先创建持久节点
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
        }
        LOGGER.info("注册服务路径（持久节点）:{}", servicePath);
        sb.append("/");
        sb.append(serviceName);
        String serviceNode = sb.toString();
        try {
            //注册包含服务地址的临时节点
            if (!zkClient.exists(serviceNode)) {
                zkClient.createEphemeral(serviceNode, serviceAddress);
            }
        } catch (ZkNodeExistsException e) {
            // 只需要保证一定有该临时节点存在即可
        }
        LOGGER.debug("注册服务节点（临时节点）：{}", serviceNode);
        //将已经注册的节点放入cache中缓存
        if (!registeredServiceMapCache.containsKey(serviceFullName)) {
            registeredServiceMapCache.put(serviceFullName, new ServiceNode(serviceName, serviceAddress));
        }
    }

    /**
     * 处理zookeeper状态变化
     *
     * @param state 状态
     * @throws Exception
     */
    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        LOGGER.info("观察到ZooKeeper状态码：{}", state.getIntValue());
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            LOGGER.info("检测到zookeeper事件:SyncConnected(连接)");
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {
            LOGGER.warn("检测到zookeeper事件:Disconnected(断开连接)");
        }
        if (state == Watcher.Event.KeeperState.Expired) {
            LOGGER.warn("检测到zookeeper事件:Expired(session过期)");
        }
    }

    /**
     * 创建新的session
     *
     * @throws Exception
     */
    @Override
    public void handleNewSession() throws Exception {
        LOGGER.info("ZooKeeper创建新的会话，重新注册节点");
        for (String serviceFullName : registeredServiceMapCache.keySet()) {
            ServiceNode serviceNode = registeredServiceMapCache.get(serviceFullName);
            registerService(serviceFullName, serviceNode.getServerName(), serviceNode.getServiceAddress());
        }
    }

    /**
     * session创建失败
     *
     * @param error
     * @throws Exception
     */
    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        LOGGER.info("handleSessionEstablishmentError:{}", error.getCause());
    }


    private static class ServiceNode {

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getServiceAddress() {
            return serviceAddress;
        }

        public void setServiceAddress(String serviceAddress) {
            this.serviceAddress = serviceAddress;
        }

        public ServiceNode(String serverName, String serviceAddress) {
            this.serverName = serverName;
            this.serviceAddress = serviceAddress;
        }

        /**
         * 服务器名称
         */
        private String serverName;

        /**
         * 服务器地址
         */
        private String serviceAddress;
    }
}
