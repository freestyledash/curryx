package com.freestyledash.curryx.client.bootstrap;

import com.freestyledash.curryx.client.RPCClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * rpcClient启动器
 * 单例
 * 持有rpc客户端实例
 */
public class RPCClientBootstrap {

    private RPCClient rpcClient;

    public synchronized static void init(String springPath) {
        if (instance == null) {
            ApplicationContext context = new ClassPathXmlApplicationContext(springPath);
            instance = new RPCClientBootstrap(context.getBean(RPCClient.class));
        }
    }

    public synchronized static void init(RPCClient rpcClient) {
        if (instance == null) {
            instance = new RPCClientBootstrap(rpcClient);
        }
    }

    public static RPCClient getRPCClient() {
        if (instance == null) {
            throw new RuntimeException("rpcClient未实例化");
        }
        return instance.rpcClient;
    }

    private static RPCClientBootstrap instance;

    private RPCClientBootstrap(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }
}
