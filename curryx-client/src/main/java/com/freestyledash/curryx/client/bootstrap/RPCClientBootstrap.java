package com.freestyledash.curryx.client.bootstrap;

import com.freestyledash.curryx.client.RPCClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * rpcClient启动器
 * 持有rpc客户端实例
 * 是一个FactorBean,通过spring配置之后可以获得RpcClient
 */
public class RPCClientBootstrap implements FactoryBean {

    private final RPCClient rpcClient;

    public RPCClientBootstrap(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public Object getObject() throws Exception {
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }


    /**
     * 希望今后的配置都通过spring来直接配置
     */
    @Deprecated
    public synchronized static void init(String springPath) {
        if (instance == null) {
            ApplicationContext context = new ClassPathXmlApplicationContext(springPath);
            instance = new RPCClientBootstrap(context.getBean(RPCClient.class));
        }
    }

    /**
     * 希望今后的配置都通过spring来直接配置
     */
    @Deprecated
    public synchronized static void init(RPCClient rpcClient) {
        if (instance == null) {
            instance = new RPCClientBootstrap(rpcClient);
        }
    }

    /**
     * 希望今后的配置都通过spring来直接配置
     */
    @Deprecated
    public synchronized static void init(ApplicationContext context) {
        if (instance == null) {
            instance = new RPCClientBootstrap(context.getBean(RPCClient.class));
        }
    }

    /**
     * 使用spring的工厂方法来获得
     *
     * @return
     */
    @Deprecated
    public static RPCClient getRPCClient() {
        if (instance == null) {
            throw new RuntimeException("rpcClient未实例化");
        }
        return instance.rpcClient;
    }

    @Deprecated
    private static RPCClientBootstrap instance;
}
