package com.freestyledash.curryx.server.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务注解，被该注解修饰的类即服务端所提供的服务
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface Service {


    /**
     * @return 服务类
     */
    Class<?> name();

    /**
     * @return 服务版本
     */
    String version() default "debug";

}
