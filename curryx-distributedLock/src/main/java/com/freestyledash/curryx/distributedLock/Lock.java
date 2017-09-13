package com.freestyledash.curryx.distributedLock;

/**
 * @author zhangyanqi
 * @since 1.0 2017/9/13
 */
public class Lock {
    private String uuid; //锁标识

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    private String resource; //锁资源名称


}
