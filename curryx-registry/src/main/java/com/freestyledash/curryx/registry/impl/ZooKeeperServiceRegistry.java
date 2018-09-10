package com.freestyledash.curryx.registry.impl;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.registry.util.Constants;
import com.freestyledash.curryx.server.server.Server;
import com.freestyledash.curryx.serviceContainer.ServiceContainer;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.freestyledash.curryx.common.constant.PunctuationConst.COMMA;

/**
 * 使用ZooKeeper名字服务器实现的服务发现
 * <p>
 * 生命周期：
 * 构造方法
 * setContainer
 * setServer
 * setName
 * connect
 * registerAll/register
 * shutdown
 *
 * @author zhangyanqi
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry, IZkStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    /**
     * 连接注册中心
     */
    @Override
    public void connect() {
        LOGGER.info("开始连接zookeepers");
        if (container == null && server == null && serverName == null) {
            throw new IllegalStateException("container or server or serverName is NUll");
        }
        if (zkAddress == null || "".equals(zkAddress)) {
            throw new IllegalArgumentException("无效的ZooKeeper地址");
        }
        if (serviceRoot == null || "".equals(serviceRoot)) {
            throw new IllegalArgumentException("无效的服务根节点");
        }
        zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
        if (zkAddress.contains(COMMA)) {
            LOGGER.info("连接到ZooKeeper服务器集群:{}", zkAddress);
        } else {
            LOGGER.info("连接到ZooKeeper单机服务器:{}", zkAddress);
        }
        zkClient.subscribeStateChanges(this);
        if (!zkClient.exists(serviceRoot)) {
            zkClient.createPersistent(serviceRoot);
        }
        LOGGER.info("服务根节点(持久节点):{}", serviceRoot);
    }

    /**
     * 服务名字
     */
    private String serverName;

    @Override
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * 通讯服务器
     */
    private Server server;

    @Override
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * ZooKeeper客户端实例
     */
    private ZkClient zkClient;

    private final String serviceRoot;

    private String zkAddress;

    private int zkSessionTimeout;

    private int zkConnectionTimeout;


    /**
     * 服务实例容器
     */
    private ServiceContainer container;

    /**
     * @param container 服务提供者
     */
    @Override
    public void setServiceContainer(ServiceContainer container) {
        this.container = container;
    }

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
        this.zkAddress = zkAddress;
        this.serviceRoot = serviceRoot;
        this.zkSessionTimeout = zkSessionTimeout;
        this.zkConnectionTimeout = zkConnectionTimeout;
    }

    /**
     * 注册服务
     * 将一个"类路径"注册到zookeeper中
     * <p>
     * 一个服务注册的预期结果为：
     * /root/serviceFullName/serverName
     * 节点内包含信息为ip:port
     * <p>
     * 其中/root/serviceFullName/为永久节点
     * serverName为临时节点
     *
     * @param serviceFullName 服务全称
     * @param serverName      服务提供者的名字
     * @param serviceAddress  提供服务的服务器的地址,格式: ip:port 例如: 127.0.0.1:8080
     */
    public void registerService(String serviceFullName, String serverName, String serviceAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceRoot).append('/').append(serviceFullName);
        String servicePath = sb.toString();
        //如果root/serviceFullName 不存在，则先创建持久节点
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
        }
        LOGGER.info("注册服务路径(持久节点):{},节点地址为{}", servicePath, serviceAddress);
        sb.append("/").append(serverName);
        String serviceNode = sb.toString();
        try {
            //注册临时节点
            if (!zkClient.exists(serviceNode)) {
                zkClient.createEphemeral(serviceNode, serviceAddress);
            }
        } catch (ZkNodeExistsException e) {
            // 只需要保证一定有该临时节点存在即可
        }
        LOGGER.info("注册服务节点(临时节点):{}", serviceNode);
    }

    /**
     * 注册所有服务
     *
     * @return 注册是否成功
     */
    @Override
    public boolean registerAllService() {
        Map<String, Object> serviceMap = container.getServiceMap();
        for (String serviceFullName : serviceMap.keySet()) {
            registerService(serviceFullName, serverName, server.getAddress());
        }
        return true;
    }

    /**
     * 关闭
     */
    @Override
    public void shutdown() {
        LOGGER.info("开始关闭服务注册");
        zkClient.close();
        LOGGER.info("服务注册已经关闭");
    }

    /**
     * 处理zookeeper状态变化
     *
     * @param state 状态
     */
    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) {
        LOGGER.info("观察到ZooKeeper状态码：{}", state.getIntValue());
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            LOGGER.info("检测到zookeeper事件:SyncConnected");
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {
            //zk宕机重启不会创建新的session
            LOGGER.warn("检测到zookeeper事件:Disconnected");
        }
        if (state == Watcher.Event.KeeperState.Expired) {
            //debug可以导致session过期
            LOGGER.warn("检测到zookeeper事件:Expired");
        }
    }

    /**
     * 创建新的session
     * 进行服务检查，检查失败则停止服务
     * 同时重新注册节点
     */
    @Override
    public void handleNewSession() {
        LOGGER.info("创建新的Session");
        if (server.checkHealth()) {
            LOGGER.info("服务器状态检查,正常运行,重新注册节点");
            for (String key : container.getServiceMap().keySet()) {
                registerService(key, serverName, server.getAddress());
            }
            return;
        }
        LOGGER.error("服务器状态检查,非正常运行,放弃节点注册,关闭服务器");
        Runtime.getRuntime().exit(0);
    }

    /**
     * session创建失败
     *
     * @param error
     */
    @Override
    public void handleSessionEstablishmentError(Throwable error) {
        LOGGER.error("handleSessionEstablishmentError:{}", error.getCause());
    }

}