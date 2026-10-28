package com.campus.runner.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 静态资源 MIME 补充。
 * <p>
 * Tomcat 默认不认识 {@code .webmanifest}，会以 {@code application/octet-stream} 返回，
 * 浏览器会因此拒绝解析 PWA manifest（“添加到主屏幕”失效）。此处显式登记正确 MIME。
 */
@Configuration
public class StaticMimeConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webManifestMimeTypeCustomizer() {
        return factory -> factory.addContextCustomizers(
                context -> context.addMimeMapping("webmanifest", "application/manifest+json"));
    }
}
