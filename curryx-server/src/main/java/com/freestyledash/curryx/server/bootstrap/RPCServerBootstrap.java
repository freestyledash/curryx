package com.freestyledash.curryx.server.bootstrap;

import com.freestyledash.curryx.server.RPCServer;
import org.springframework.context.ApplicationContext;


/**
 * server启动类
 */
public class RPCServerBootstrap {

    /**
     * 启动server
     *
     * @param context spring实例
     */
    public static void launch(ApplicationContext context) {
        context.getBean(RPCServer.class).start();
    }

    /**
     * 新建线程启动server
     *
     * @param context spring实例
     */
    public static void launchInNewThread(ApplicationContext context) {
        new Thread(() -> {
            launch(context);
        }).start();
    }

    /**
     * 启动server和client
     *
     * @param context
     */
    public static void launchAll(ApplicationContext context) {
        RPCServer rpcServer = context.getBean(RPCServer.class);
        rpcServer.start();
    }

    /**
     * 在新的线程中启动server和client
     *
     * @param context spring实例
     */
    public static void launchAllInNewThread(ApplicationContext context) {
        new Thread(() -> {
            launchAll(context);
        }).start();
    }

}
