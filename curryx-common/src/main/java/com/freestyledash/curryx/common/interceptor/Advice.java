package com.freestyledash.curryx.common.interceptor;

/**
 * @author zhangyanqi
 * @since 1.0 2018/8/7
 * <p>
 * aop通知
 */
public interface Advice {

    void before();

    void after();

    void encounterException(Exception e);

}
