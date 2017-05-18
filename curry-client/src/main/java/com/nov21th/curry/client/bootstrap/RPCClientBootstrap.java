package com.nov21th.curry.client.bootstrap;

import com.nov21th.curry.client.RPCClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/6.
 */
public class RPCClientBootstrap {

    private static String springPath;

    private static RPCClientBootstrap instance;

    private RPCClient rpcClient;

    private RPCClientBootstrap(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public synchronized static RPCClientBootstrap getInstance(String springPath) {
        if (instance == null) {
            ApplicationContext context = new ClassPathXmlApplicationContext(springPath);
            instance = new RPCClientBootstrap(context.getBean(RPCClient.class));
        }

        return instance;
    }

    public static RPCClient getRPCClient() {
        if (instance == null) {
            return null;
        }
        return instance.rpcClient;
    }


}
