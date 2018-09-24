package com.freestyledash.curryx.demo.api;

/**
 * @author zhangyanqi
 * @since 1.0 2018/9/24
 */
public class PongEntry {

    private String msg;

    public PongEntry(String msg) {
        this.msg = msg;
    }


    @Override
    public String toString() {
        return "PongEntry{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
