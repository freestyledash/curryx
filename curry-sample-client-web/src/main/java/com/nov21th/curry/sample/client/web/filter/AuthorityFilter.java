package com.nov21th.curry.sample.client.web.filter;



import com.nov21th.curry.sample.client.web.constant.Constants;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * @author 郭永辉
 * @since 1.0 2017/3/27.
 */
public class AuthorityFilter implements Filter {


    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        HttpSession session = request.getSession();

        String url = request.getRequestURI();

        System.out.println("session id: " + session.getId());
        System.out.println("request url: " + url);

        if (url.endsWith("login.html") || url.endsWith("login.action")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            String username = (String) session.getAttribute(Constants.ATTR_LOGIN_USER);

            if (username == null || "".equals(username)) {
                response.sendRedirect(request.getContextPath() + "/login.html");
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    public void destroy() {

    }
}
