package com.nov21th.curry.common.protocol.entity;

import java.util.UUID;

/**
 * RPC通讯请求报文的格式
 * <p>
 * 请求报文中包含了一个id用于标识一次请求
 * 通过服务的名称和版本来定位所需服务的具体实现
 * 通过调用的方法名称以及参数类型来定位服务具体实现中要调用的方法
 *
 * @author 郭永辉
 * @since 1.0 2017/4/2.
 */
public class RPCRequest {

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务版本
     */
    private String serviceVersion;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 方法接受的参数类型
     */
    private Class<?>[] argsTypes;

    /**
     * 对应于方法接受的参数类型的参数值
     */
    private Object[] argsValues;

    public RPCRequest() {
        requestId = UUID.randomUUID().toString();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getArgsTypes() {
        return argsTypes;
    }

    public void setArgsTypes(Class<?>[] argsTypes) {
        this.argsTypes = argsTypes;
    }

    public Object[] getArgsValues() {
        return argsValues;
    }

    public void setArgsValues(Object[] argsValues) {
        this.argsValues = argsValues;
    }
}
