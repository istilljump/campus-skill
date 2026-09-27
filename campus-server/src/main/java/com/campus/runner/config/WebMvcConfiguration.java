package com.campus.runner.config;

import com.campus.runner.interceptor.JwtTokenAdminInterceptor;
import com.campus.runner.interceptor.JwtTokenRunnerInterceptor;
import com.campus.runner.interceptor.JwtTokenUserInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Autowired
    private JwtTokenRunnerInterceptor jwtTokenRunnerInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status");

        registry.addInterceptor(jwtTokenRunnerInterceptor)
                .addPathPatterns("/runner/**")
                .excludePathPatterns("/runner/auth/login");
    }

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket adminDocket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("校园跑腿项目接口文档")
                .version("2.0")
                .description("校园跑腿项目接口文档")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("管理端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.campus.runner.controller.admin"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket userDocket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("校园跑腿项目接口文档")
                .version("2.0")
                .description("校园跑腿项目接口文档")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("用户端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.campus.runner.controller.user"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket runnerDocket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("校园跑腿项目接口文档")
                .version("2.0")
                .description("校园跑腿项目接口文档")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("跑腿员端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.campus.runner.controller.runner"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        //前端静态页面（管理端/用户端H5/跑腿员端H5），目录入口由 PageController 重定向
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
}
