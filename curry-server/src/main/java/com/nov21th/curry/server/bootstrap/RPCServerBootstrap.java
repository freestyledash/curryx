package com.nov21th.curry.server.bootstrap;

import com.nov21th.curry.client.RPCClient;
import com.nov21th.curry.client.bootstrap.RPCClientBootstrap;
import com.nov21th.curry.server.RPCServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/6.
 */
public class RPCServerBootstrap {

    public static void launch(String springPath) {
        RPCServer rpcServer = new ClassPathXmlApplicationContext(springPath).getBean(RPCServer.class);
        rpcServer.start();
    }

    public static void launchInNewThread(final String springPath) {
        new Thread() {
            @Override
            public void run() {
                launch(springPath);
            }
        }.start();
    }

    public static void launchAll(String springPath) {
        ApplicationContext context = new ClassPathXmlApplicationContext(springPath);

        RPCClient rpcClient = context.getBean(RPCClient.class);
        RPCClientBootstrap.init(rpcClient);

        RPCServer rpcServer = context.getBean(RPCServer.class);
        rpcServer.start();
    }

    public static void launchAllInNewThread(final String springPath) {
        new Thread() {
            @Override
            public void run() {
                launchAllInNewThread(springPath);
            }
        }.start();
    }

}
