package com.freestyledash.curryx.client.bootstrap;

import com.freestyledash.curryx.client.RPCClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/6.
 */
public class RPCClientBootstrap {

    private static RPCClientBootstrap instance;

    private RPCClient rpcClient;

    private RPCClientBootstrap(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

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
            return null;
        }
        return instance.rpcClient;
    }


}
