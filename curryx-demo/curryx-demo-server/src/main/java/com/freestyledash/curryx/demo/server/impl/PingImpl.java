package com.freestyledash.curryx.demo.server.impl;

import com.freestyledash.curryx.demo.api.Ping;
import com.freestyledash.curryx.demo.api.PingEntry;
import com.freestyledash.curryx.demo.api.PongEntry;
import com.freestyledash.curryx.serviceContainer.impl.spring.Service;

/**
 * @author zhangyanqi
 * @since 1.0 2018/9/24
 */

@Service(name = Ping.class)
public class PingImpl implements Ping {

    public PongEntry Ping(PingEntry pingEntryx) {
        return new PongEntry("pong");
    }
}
