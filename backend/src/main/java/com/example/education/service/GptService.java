package com.example.education.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GptService {

    @Value("${openai.api.key}")
    private String apiKey;

    public String getGptResponse(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of(
                "model", "ft:gpt-4.1-2025-04-14:personal:math-v1:BRFAzPw7",
                "messages", List.of(message),
                "temperature", 0.7
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map choice = (Map) ((List) response.getBody().get("choices")).get(0);
            Map messageObj = (Map) choice.get("message");
            return (String) messageObj.get("content");
        } else {
            return "오류: GPT 응답 실패";
        }
    }
}