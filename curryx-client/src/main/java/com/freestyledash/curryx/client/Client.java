package com.freestyledash.curryx.client;

import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.protocol.entity.RPCResponse;

/**
 * 请求发送工具
 *
 * @author zhangyanqi
 * @since 1.0 2018/8/30
 */
public interface Client {

    public RPCResponse sendRequest(String host, int port, RPCRequest request) throws Exception;
}
