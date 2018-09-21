package com.freestyledash.curryx.serviceContainer.impl.spring;

import java.lang.annotation.*;

/**
 * 注解rpc的方法
 *
 * @author zhangyanqi
 * @since 1.0 2018/9/20
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Inherited
public @interface RpcMethod {

    /**
     * 方法实现的接口
     *
     * @return inter
     */
    Class interface_();

    /**
     * 方法名字
     *
     * @return methodName
     */
    String methodName();

    /**
     * 方法参数
     *
     * @return parameterTypes
     */
    Class[] parameterTypes();

    /**
     * 返回值类型
     *
     * @return returnType
     */
    String returnType();

    /**
     * 版本
     *
     * @return version
     */
    String version();

}
