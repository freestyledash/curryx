package com.freestyledash.curryx.registry.impl;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.registry.constant.Constants;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用ZooKeeper实现的服务发现
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry, IZkStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    /**
     * ZooKeeper客户端实例
     * <p>
     * 用于连接ZooKeeper服务器并根据要注册的服务创建相应节点
     * <p>
     * 注册完服务后不能关闭该ZooKeeper客户端的连接，这是由于要进行心跳检测以确定提供服务的服务器是否掉线
     */
    private final ZkClient zkClient;

    /**
     * 所有服务在ZooKeeper下的根节点
     * <p>
     * 例如，若根节点为/com.nov21th，则所有服务都托管在ZooKeeper服务器的/com.nov21th/路径下
     */
    private final String serviceRoot;

    private Map<String, String> serviceMap;

    private AtomicBoolean reRegister;

    public ZooKeeperServiceRegistry(String zkAddress, String serviceRoot) {
        this(zkAddress, serviceRoot, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

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

        this.zkClient.subscribeStateChanges(this);

        if (zkAddress.contains(",")) {
            logger.debug("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.debug("连接到ZooKeeper单机服务器：{}", zkAddress);
        }

        if (!zkClient.exists(serviceRoot)) {
            zkClient.createPersistent(serviceRoot);
        }

        logger.debug("服务根节点（持久节点）：{}", serviceRoot);
    }

    public void registerService(String name, String version, String serverAddress) {
        registerService(name + Constants.SERVICE_SEP + version, serverAddress);
    }

    public void registerService(String serviceFullName, String serverAddress) {
        StringBuilder sb = new StringBuilder();

        sb.append(serviceRoot);
        sb.append('/');
        sb.append(serviceFullName);

        String servicePath = sb.toString();
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
        }

        logger.debug("注册服务路径（持久节点）：{}", servicePath);

        sb.append("/");
        sb.append(serverAddress);

        String serviceNode = sb.toString();

        try {
            //注册包含服务地址的临时节点，ZooKeeper客户端断线后该节点会自动被ZooKeeper服务器删除
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
        logger.debug("观察到ZooKeeper状态码：{}", state.toString());
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            logger.debug("向zookeeper重新注册服务集合");
            for (String serviceFullName : serviceMap.keySet()) {
                String serverAddress = serviceMap.get(serviceFullName);
                registerService(serviceFullName, serverAddress);
            }
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {

        }
        if (state == Watcher.Event.KeeperState.Expired) {

        }
    }

    /**
     * 创建新的session
     *
     * @throws Exception
     */
    @Override
    public void handleNewSession() throws Exception {
        logger.debug("ZooKeeper会话过期，创建新的会话，重新注册节点");
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
        logger.debug("handleSessionEstablishmentError:{}", error.getCause());
    }
}
