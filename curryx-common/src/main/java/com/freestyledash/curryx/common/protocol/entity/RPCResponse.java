package com.freestyledash.curryx.common.protocol.entity;

/**
 * RPC通讯响应报文的格式
 * 包含对应于请求报文的id
 * 若服务成功调用则需要将调用的结果存于<code>result</code>字段中
 * 否则，若调用过程中发生异常则需要将异常存于<code>exception</code>字段中
 *
 * @author zhangyanqi
 */
@SuppressWarnings("ALL")
public class RPCResponse {

    /**
     * 对应于请求id
     */
    private String requestId;

    /**
     * 请求调用的结果
     */
    private Object result;

    /**
     * 请求调用的异常
     */
    private Exception exception;

    public RPCResponse() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }


    @Override
    public String toString() {
        return "RPCResponse{" +
                "requestId='" + requestId + '\'' +
                ", result=" + result +
                ", exception=" + exception +
                '}';
    }
}
