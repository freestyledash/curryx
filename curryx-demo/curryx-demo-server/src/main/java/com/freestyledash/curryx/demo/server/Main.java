package com.freestyledash.curryx.demo.server;

        import com.freestyledash.curryx.rpcServer.RPCServer;
        import org.springframework.context.ApplicationContext;
        import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author zhangyanqi
 * @since 1.0 2018/9/24
 */
public class Main {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("app.xml");
        RPCServer serverBootstrap = context.getBean(RPCServer.class);
        serverBootstrap.start();
    }
}
