package com.freestyledash.curryx.server.bootstrap;

import com.freestyledash.curryx.server.RPCServer;


/**
 * server启动类
 *
 * @author zhangyanqi
 */
public class RPCServerBootstrap {

    private RPCServer rpcServer;

    public RPCServerBootstrap(RPCServer rpcServer) {
        this.rpcServer = rpcServer;
    }

    /**
     * 在当前线程启动server
     */
    public void launch() {
        this.rpcServer.start();
    }

}
