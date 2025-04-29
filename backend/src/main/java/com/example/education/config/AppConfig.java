package com.example.education.config;

import com.example.education.service.GptService;
import com.example.education.util.GptResponseValidator;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final GptService gptService;

    @PostConstruct
    public void init() {
        GptResponseValidator.setGptService(gptService);
    }
}
