package com.nov21th.curry.sample.server.impl;

import com.nov21th.curry.sample.api.UserService;
import com.nov21th.curry.server.annotation.Service;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/5.
 */
@Service(name = UserService.class, version = "withNoDB")
public class UserServiceImplWithNoDB implements UserService {

    @Override
    public boolean login(String username, String password) {
        return "admin".equals(username) && "admin".equals(password);
    }
}
