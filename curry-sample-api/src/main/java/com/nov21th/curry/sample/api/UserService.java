package com.nov21th.curry.sample.api;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/5.
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return true 登陆成功 false 登陆失败
     */
    boolean login(String username, String password);

}
