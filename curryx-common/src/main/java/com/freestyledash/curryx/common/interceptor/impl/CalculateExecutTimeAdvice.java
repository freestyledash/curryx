package com.freestyledash.curryx.common.interceptor.impl;

import com.freestyledash.curryx.common.interceptor.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author zhangyanqi
 * @since 1.0 2018/8/8
 * <p>
 * 统计时间
 */
public class CalculateExecutTimeAdvice implements Advice {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculateExecutTimeAdvice.class);

    /**
     * 耗时
     */
    private ThreadLocal<Long> time = new ThreadLocal<Long>();

    @Override
    public void before() {
        time.set(System.currentTimeMillis());
    }

    @Override
    public void after() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("执行耗时{}", System.currentTimeMillis() - this.time.get());
        }
    }

    @Override
    public void encounterException(Exception e) {

    }
}
