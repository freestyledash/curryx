package com.nov21th.curry.server.bootstrap;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/6.
 */
public class RPCServerBootstrap {

    public static void launchRPCServer() {
        new ClassPathXmlApplicationContext("spring.xml");
    }

}
