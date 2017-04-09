package com.nov21th.curry.sample.client.web.listener;


import com.nov21th.curry.client.bootstrap.RPCClientBootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author 郭永辉
 * @since 1.0 2017/3/28.
 */
public class InitializeListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        RPCClientBootstrap.getInstance();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
