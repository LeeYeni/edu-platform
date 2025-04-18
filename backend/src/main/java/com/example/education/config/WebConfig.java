package com.example.education.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 홈
        registry.addViewController("/").setViewName("forward:/index.html");

        // React SPA fallback
        registry.addViewController("/{spring:[a-zA-Z0-9-_]+}")
                .setViewName("forward:/index.html");

        // 정적 자원(js, css 등)은 제외한 모든 경로 fallback
        registry.addViewController("/{spring:[a-zA-Z0-9-_]+}/**{spring:?!(\\.js|\\.css|\\.png|\\.jpg|\\.jpeg|\\.svg|\\.json|\\.woff2|\\.map)$}")
                .setViewName("forward:/index.html");
    }
}
