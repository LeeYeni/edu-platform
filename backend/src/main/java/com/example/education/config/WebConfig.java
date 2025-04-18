package com.example.education.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 루트 경로 (홈)
        registry.addViewController("/").setViewName("forward:/index.html");

        // React SPA fallback 경로 설정
        registry.addViewController("/{spring:[a-zA-Z0-9-_]+}")
                .setViewName("forward:/index.html");

        // 하위 경로 중 정적 리소스를 제외한 모든 요청만 처리
        registry.addViewController("/**/{spring:[a-zA-Z0-9-_]+}")
                .setViewName("forward:/index.html");

        // ✅ 여기 정규식에서 .js, .css, .svg, .json, .woff2 등 꼭 예외 처리해야 합니다!
        registry.addViewController("/{spring:[a-zA-Z0-9-_]+}/**{spring:?!(\\.js|\\.css|\\.png|\\.jpg|\\.svg|\\.json|\\.woff2|\\.map)$}")
                .setViewName("forward:/index.html");
    }
}
