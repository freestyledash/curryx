package com.nov21th.curry.sample.server.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/5.
 */
public class DBProxy {


    private static final Map<Class<?>, Object> cachedProxy;

    private static SqlSessionFactory sessionFactory;

    static {
        cachedProxy = new ConcurrentHashMap<>();

        try {
            Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
            sessionFactory = new SqlSessionFactoryBuilder().build(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<?> mapperClass) {
        Object proxy = cachedProxy.get(mapperClass);

        if (proxy == null) {
            proxy = Proxy.newProxyInstance(
                    mapperClass.getClassLoader(),
                    new Class<?>[]{mapperClass},
                    new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            SqlSession session = sessionFactory.openSession(true);

                            return method.invoke(session.getMapper(mapperClass), args);
                        }
                    }
            );

            cachedProxy.put(mapperClass, proxy);
        }

        return (T) proxy;
    }

}
