package com.freestyledash.curryx.server;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.registry.constant.Constants;
import com.freestyledash.curryx.server.annotation.Service;
import com.freestyledash.curryx.server.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务端
 * 实现ApplicationContextAware
 * 在该类被spring初始化后会执行setApplicationContext方法
 */
public class RPCServer implements ApplicationContextAware {


    private static final Logger logger = LoggerFactory.getLogger(RPCServer.class);

    /**
     * 通讯服务器
     */
    private Server server;

    /**
     * 本机网络进程的地址，格式为ip:port
     */
    private String serverAddress;


    /**
     * 注册服务的接口
     */
    private ServiceRegistry serviceRegistry;

    /**
     * 保存服务容器
     */
    private Map<String, Object> serviceMap = new HashMap();

    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry, Server server) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
        this.server = server;
    }

    /**
     * 通过spring扫描获得所有被标记为service的类,并把它们放入serviceMap中
     *
     * @param context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        //扫描指定路径下被Service注解修饰的类
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
        //若扫描到的map为空则说明当前服务器没有提供任何服务，警告
        if (map == null || map.size() == 0) {
            logger.warn("在当前服务器下没有任何服务");
            return;
        }
        //对扫描到的每一个service，记录其服务名称和版本
        for (Object serviceBean : map.values()) {
            Service serviceAnnotation = serviceBean.getClass().getAnnotation(Service.class);
            String serviceFullName = serviceAnnotation.name().getName() + Constants.SERVICE_SEP + serviceAnnotation.version();
            serviceMap.put(serviceFullName, serviceBean);
            logger.debug("扫描到服务：{}", serviceFullName);
        }
    }

    /**
     * 启动服务
     * 启动netty
     * 注册服务
     */
    public void start() {
        this.server.start();
        registerServices();//将服务注册到名字服务器中
    }

    /**
     * 注册服务
     */
    private void registerServices() {
        if (serviceRegistry != null) {
            for (String serviceFullName : serviceMap.keySet()) {
                logger.debug("向注册中心注册服务：{}", serviceFullName);
                serviceRegistry.registerService(serviceFullName, serverAddress);
            }
        } else {
            throw new RuntimeException("服务中心不可用");
        }
    }
}
