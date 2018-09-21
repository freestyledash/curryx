package com.freestyledash.curryx.serviceContainer.impl.spring;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被该注解修饰的类会被实例化，期中rpc的方法需要使用@RpcMethod注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface ServiceProvider {
}
