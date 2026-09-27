package com.campus.runner.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 前端页面入口跳转：目录形式访问时重定向到各端首页
 * （项目关闭了视图解析器，使用 sendRedirect 绕开视图层）
 */
@RestController
public class PageController {

    @GetMapping({"/", "/admin-app", "/admin-web", "/user-app", "/runner-app"})
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        // 去掉尾部斜杠，避免拼接出 /xxx//index.html
        while (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        if ("/".equals(uri)) {
            //根路径由静态资源 index.html 直接服务，此处兜底
            response.sendRedirect("/index.html");
        } else {
            response.sendRedirect(uri + "/index.html");
        }
    }
}
