package com.freestyledash.curryx.demo.client;

import com.freestyledash.curryx.demo.api.Ping;
import com.freestyledash.curryx.demo.api.PingEntry;
import com.freestyledash.curryx.demo.api.PongEntry;
import com.freestyledash.curryx.rpcClient.RPCClient;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author zhangyanqi
 * @since 1.0 2018/9/24
 */
public class Main {

    public static void main(String[] args) {
        RPCClient client = new ClassPathXmlApplicationContext("app.xml").getBean(RPCClient.class);
        Ping t = client.create(Ping.class, "debug");
        PingEntry ping = new PingEntry("ping");
        PongEntry response = t.Ping(ping);
        System.out.println(response);
    }
}
