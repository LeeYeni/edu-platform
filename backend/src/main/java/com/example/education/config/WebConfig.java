package com.example.education.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 루트 경로
        registry.addViewController("/").setViewName("forward:/index.html");

        // 확장자 없는 단일 경로 (예: /login, /mypage 등)
        registry.addViewController("/{x:[\\w\\-]+}")
                .setViewName("forward:/index.html");

        // 중첩 경로 (예: /quiz/room/123, /quiz/play/456)
        registry.addViewController("/{x:^(?!api$).*$}/**/{y:[\\w\\-]+}")
                .setViewName("forward:/index.html");
    }
}

