package com.nov21th.curry.sample.client.web.action;

import com.nov21th.curry.client.RPCClient;
import com.nov21th.curry.client.bootstrap.RPCClientBootstrap;
import com.nov21th.curry.sample.api.UserService;
import com.nov21th.curry.sample.client.web.constant.Constants;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

import javax.servlet.http.HttpSession;

/**
 * @author 郭永辉
 * @since 1.0 2017/3/27.
 */
public class LoginAction extends ActionSupport {

    private static int count = 0;

    private String username;

    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String execute() throws Exception {
        RPCClient proxy = RPCClientBootstrap.getInstance().getRPCClient();
        UserService service = proxy.create(UserService.class, "withDB");

        if (service.login(username, password)) {
            HttpSession session = ServletActionContext.getRequest().getSession();
            session.setAttribute(Constants.ATTR_LOGIN_USER, "admin-" + session.getId());

            return SUCCESS;
        } else {
            return ERROR;
        }
    }
}
