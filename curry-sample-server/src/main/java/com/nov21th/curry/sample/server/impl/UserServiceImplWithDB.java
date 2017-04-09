package com.nov21th.curry.sample.server.impl;

import com.nov21th.curry.sample.api.UserService;
import com.nov21th.curry.sample.model.User;
import com.nov21th.curry.sample.server.mapper.UserMapper;
import com.nov21th.curry.sample.server.util.DBProxy;
import com.nov21th.curry.server.annotation.Service;

/**
 * @author 郭永辉
 * @since 1.0 2017/4/5.
 */
@Service(name = UserService.class, version = "withDB")
public class UserServiceImplWithDB implements UserService {

    @Override
    public boolean login(String username, String password) {
        UserMapper mapper = DBProxy.create(UserMapper.class);

        User user = mapper.selectByPrimaryKey(username);

        return user != null && user.getPassword().equals(password);
    }
}
