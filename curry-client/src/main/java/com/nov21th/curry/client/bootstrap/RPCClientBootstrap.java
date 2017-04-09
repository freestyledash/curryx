package com.nov21th.curry.client.bootstrap;

import com.nov21th.curry.client.RPCClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/6.
 */
public class RPCClientBootstrap {

    private final RPCClient rpcClient;

    private RPCClientBootstrap(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public static RPCClientBootstrap getInstance() {
        return SingletonHolder.instance;
    }

    public RPCClient getRPCClient() {
        return rpcClient;
    }

    private static class SingletonHolder {

        private static RPCClientBootstrap instance;

        static {
            ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
            instance = new RPCClientBootstrap(context.getBean(RPCClient.class));
        }

    }

}
