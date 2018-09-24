package com.freestyledash.curryx.demo.api;

/**
 * @author zhangyanqi
 * @since 1.0 2018/9/24
 */
public class PingEntry {

    private String msg;

    public PingEntry(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "PingEntry{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
