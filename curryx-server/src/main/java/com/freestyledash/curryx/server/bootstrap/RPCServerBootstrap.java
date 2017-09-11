package com.freestyledash.curryx.server.bootstrap;

import com.freestyledash.curryx.client.RPCClient;
import com.freestyledash.curryx.client.bootstrap.RPCClientBootstrap;
import com.freestyledash.curryx.server.RPCServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * server启动类
 */
public class RPCServerBootstrap {

    /**
     * 启动server
     *
     * @param springPath
     */
    public static void launch(String springPath) {
        RPCServer rpcServer = new ClassPathXmlApplicationContext(springPath).getBean(RPCServer.class);
        rpcServer.start();
    }

    /**
     * 新建线程启动server
     *
     * @param springPath
     */
    public static void launchInNewThread(final String springPath) {
        new Thread(() -> {
            launch(springPath);
        }).start();
    }

    /**
     * 启动server和client
     *
     * @param springPath
     */
    public static void launchAll(String springPath) {
        ApplicationContext context = new ClassPathXmlApplicationContext(springPath);

        RPCClient rpcClient = context.getBean(RPCClient.class);
        RPCClientBootstrap.init(rpcClient);

        RPCServer rpcServer = context.getBean(RPCServer.class);
        rpcServer.start();
    }

    /**
     * 在新的线程中启动server和client
     *
     * @param springPath
     */
    public static void launchAllInNewThread(final String springPath) {
        new Thread(() -> {
            launchAll(springPath);
        }).start();
    }

}
