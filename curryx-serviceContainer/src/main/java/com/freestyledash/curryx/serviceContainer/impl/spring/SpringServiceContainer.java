package com.freestyledash.curryx.serviceContainer.impl.spring;

import com.freestyledash.curryx.serviceContainer.ServiceContainer;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.concurrent.*;

import static com.freestyledash.curryx.common.constant.PunctuationConst.STRIGULA;

/**
 * 使用sping进行service实例化
 *
 * @author zhangyanqi
 * @since 1.0 2018/8/29
 */
public class SpringServiceContainer implements ServiceContainer, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringServiceContainer.class);

    private ApplicationContext context;

    /**
     * 组件加载使用的线程池
     */
    private ExecutorService threadPoolExecutor;

    private Map<String, Object> serviceMap;

    public SpringServiceContainer() {
        serviceMap = new ConcurrentHashMap<>(20);
        threadPoolExecutor = new ThreadPoolExecutor(
                5,
                10,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                new DefaultThreadFactory("组件加载用线程池"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
    }

    /**
     * 加载服务
     *
     * @return 服务加载是否成功
     */
    @Override
    public boolean load() {
        if (context == null) {
            throw new IllegalStateException("没有加载springContext");
        }
        LOGGER.info("开始实例化服务对象");
        //扫描指定路径下被Service注解修饰的类
        Map<String, Object> map = context.getBeansWithAnnotation(Service.class);
        //若扫描到的map为空则说明当前服务器没有提供任何服务，警告
        if (map == null || map.size() == 0) {
            LOGGER.warn("在当前服务器下没有任何服务");
            return false;
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
        LOGGER.info("服务实例化结束");
        return true;
    }

    /**
     * 返回加载的服务
     *
     * @param serviceName
     * @return
     */
    @Override
    public Object get(String serviceName) {
        return serviceMap.get(serviceName);
    }

    /**
     * 获得加载的所有服务
     *
     * @return
     */
    @Override
    public Map<String, Object> getServiceMap() {
        return this.serviceMap;
    }
}
