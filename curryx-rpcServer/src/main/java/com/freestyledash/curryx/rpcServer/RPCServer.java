package com.freestyledash.curryx.rpcServer;

import com.freestyledash.curryx.registry.ServiceRegistry;
import com.freestyledash.curryx.server.annotation.Service;
import com.freestyledash.curryx.server.server.Server;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static com.freestyledash.curryx.common.constant.PunctuationConst.STRIGULA;

/**
 * RPC服务端
 * 职责：
 * 加载服务
 * 启动服务
 * 注册服务
 * <p>
 * 实现ApplicationContextAware
 * 在该类被spring初始化后会执行setApplicationContext方法
 */
public class RPCServer implements ApplicationContextAware {


    private static final Logger LOGGER = LoggerFactory.getLogger(RPCServer.class);

    /**
     * 组件加载使用的线程池
     */
    private ExecutorService threadPoolExecutor;

    /**
     * 服务器名字，在注册服务的时候，服务节点名字为该名字，默认为{ip}:{一个随机的UUID},不推荐使用
     */
    private String serverName;

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
     * key:serviceFullName
     * value:ServiceObject
     */
    private Map<String, Object> serviceMap;

    private void init() {
        threadPoolExecutor = new ThreadPoolExecutor(
                5,
                10,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                new DefaultThreadFactory("组件加载线程池"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        serviceMap = new ConcurrentHashMap<>(20);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.shutdown();
        }, "rpcServerShutDownHook"));
    }

    public RPCServer(ServiceRegistry serviceRegistry, Server server) {
        this.serviceRegistry = serviceRegistry;
        this.server = server;
        this.serverName = UUID.randomUUID().toString();
        this.serverAddress = server.getAddress();
        this.serviceRegistry.setServer(server);
        init();
    }

    public RPCServer(ServiceRegistry serviceRegistry, Server server, String serverName) {
        this.serviceRegistry = serviceRegistry;
        this.server = server;
        this.serverName = serverName;
        this.serverAddress = server.getAddress();
        this.serviceRegistry.setServer(server);
        init();
    }

    /**
     * 通过spring扫描获得所有被标记为service的类,并把它们放入serviceMap中
     *
     * @param context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        LOGGER.info("开始实例化服务对象...");
        //扫描指定路径下被Service注解修饰的类
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
        //若扫描到的map为空则说明当前服务器没有提供任何服务，警告
        if (map == null || map.size() == 0) {
            LOGGER.warn("在当前服务器下没有任何服务");
            return;
        }
        //对扫描到的每一个service，记录其服务名称和版本
        final CountDownLatch countDownLatch = new CountDownLatch(map.size());
        //对扫描到的每一个service，记录其服务名称和版本
        for (Object serviceBean : map.values()) {
            threadPoolExecutor.execute(() -> {
                if (Thread.interrupted()) {
                    threadPoolExecutor.shutdownNow();
                    return;
                }
                Service serviceAnnotation = serviceBean.getClass().getAnnotation(Service.class);
                String serviceFullName = serviceAnnotation.name().getName() + STRIGULA + serviceAnnotation.version();
                LOGGER.info("实例化服务:{}", serviceFullName);
                serviceMap.put(serviceFullName, serviceBean);
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
            threadPoolExecutor.shutdownNow();
        } catch (InterruptedException e) {
            threadPoolExecutor.shutdownNow();
            throw new IllegalThreadStateException("因为异常而停止实例化");
        } finally {
            if (!threadPoolExecutor.isShutdown()) {
                threadPoolExecutor.shutdownNow();
            }
        }
        this.server.setServiceMap(serviceMap);
        LOGGER.info("服务实例化结束");
    }

    /**
     * 在新线程中启动netty
     * 将服务注册到名字服务器中
     * 只有服务启动了，才能注册服务到名字服务器中
     */
    public void start() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(
                () -> this.server.start(countDownLatch)).start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("启动netty失败");
            return;
        }
        registerServices();
    }

    /**
     * 注册服务
     */
    private void registerServices() {
        if (serviceRegistry != null) {
            for (String serviceFullName : serviceMap.keySet()) {
                LOGGER.debug("向注册中心注册服务：{}", serviceFullName);
                serviceRegistry.registerService(serviceFullName, serverName, serverAddress);
            }
        } else {
            throw new RuntimeException("服务中心不可用");
        }
    }

    /**
     * 停止服务器
     */
    public void shutdown() {
        LOGGER.info("服务关闭中...");
        this.serviceRegistry.shutdown();
        this.server.shutdown();
        LOGGER.info("服务关闭完成");
    }
}
