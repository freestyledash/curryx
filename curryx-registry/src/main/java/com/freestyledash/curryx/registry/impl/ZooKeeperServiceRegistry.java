package com.freestyledash.curryx.registry.impl;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.registry.util.constant.Constants;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.freestyledash.curryx.registry.util.constant.Constants.COMMA;

/**
 * 使用ZooKeeper名字服务器实现的服务发现
 *
 * @author zhangyanqi
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry, IZkStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    /**
     * ZooKeeper客户端实例
     */
    private final ZkClient zkClient;

    /**
     * 所有服务在ZooKeeper下的根节点
     */
    private final String serviceRoot;

    /**
     * 储存被发布到zookeeper中的服务名称和服务地址
     */
    private Map<String, String> serviceMap;

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
        this.serviceMap = new HashMap<>();
        this.serviceRoot = serviceRoot;
        this.zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
        if (zkAddress.contains(COMMA)) {
            logger.info("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.info("连接到ZooKeeper单机服务器：{}", zkAddress);
        }
        this.zkClient.subscribeStateChanges(this);
        if (!zkClient.exists(serviceRoot)) {
            zkClient.createPersistent(serviceRoot);
        }
        logger.info("服务根节点（持久节点）：{}", serviceRoot);
    }

    /**
     * 注册服务
     *
     * @param name          服务名称
     * @param version       服务版本
     * @param serverAddress 提供服务的服务器的地址
     */
    @Override
    public void registerService(String name, String version, String serverAddress) {
        registerService(name + Constants.SERVICE_SEP + version, serverAddress);
    }

    /**
     * 注册服务
     *
     * @param serviceFullName 服务全称
     * @param serverAddress   提供服务的服务器的地址
     */
    @Override
    public void registerService(String serviceFullName, String serverAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceRoot);
        sb.append('/');
        sb.append(serviceFullName);
        String servicePath = sb.toString();
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
        }
        logger.info("注册服务路径（持久节点）：{}", servicePath);
        sb.append("/");
        sb.append(serverAddress);
        String serviceNode = sb.toString();
        try {
            //注册包含服务地址的临时节点
            if (!zkClient.exists(serviceNode)) {
                zkClient.createEphemeral(serviceNode, serverAddress);
            }
        } catch (ZkNodeExistsException e) {
            // do nothing
            // 只需要保证一定有该临时节点存在即可
        }
        logger.debug("注册服务节点（临时节点）：{}", serviceNode);
        if (!serviceMap.containsKey(serviceFullName)) {
            serviceMap.put(serviceFullName, serverAddress);
        }
    }

    /**
     * 处理zookeeper状态变化
     *
     * @param state
     * @throws Exception
     */
    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        logger.info("观察到ZooKeeper状态码：{}", state.getIntValue());
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            logger.info("检测到zookeeper事件:SyncConnected(连接)");
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {
            logger.warn("检测到zookeeper事件:Disconnected(断开连接)");
        }
        if (state == Watcher.Event.KeeperState.Expired) {
            logger.warn("检测到zookeeper事件:Expired(session过期)");
        }
    }

    /**
     * 创建新的session
     *
     * @throws Exception
     */
    @Override
    public void handleNewSession() throws Exception {
        logger.info("ZooKeeper创建新的会话，重新注册节点");
        for (String serviceFullName : serviceMap.keySet()) {
            String serverAddress = serviceMap.get(serviceFullName);
            registerService(serviceFullName, serverAddress);
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
        logger.info("handleSessionEstablishmentError:{}", error.getCause());
    }
}
