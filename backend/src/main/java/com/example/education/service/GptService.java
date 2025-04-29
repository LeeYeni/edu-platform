package com.example.education.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static com.example.education.util.GptResponseValidator.extractAnswerIdFromExplanation;

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

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            System.out.println("📦 전체 응답: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map messageObj = (Map) choices.get(0).get("message");
                    if (messageObj != null) {
                        return (String) messageObj.get("content");
                    } else {
                        System.out.println("❗ messageObj가 null입니다.");
                    }
                } else {
                    System.out.println("❗ choices가 비어있습니다.");
                }
            } else {
                System.out.println("❗ GPT 응답 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ GPT 호출 중 예외 발생: " + e.getMessage());
        }

        return "";
    }

    public Map<String, String> solveProblemAndExtractAnswer(String questionText, List<Map<String, String>> options) {
        for (Map<String, String> option : options) {
            String text = option.get("text");
            if (text != null) {
                option.put("text", text.replaceAll("\\(오답\\)", "").trim());
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 수학 문제의 정답을 계산하고, 보기 중 정답과 정확히 일치하는 보기의 텍스트(text)만 아래 JSON 형식으로 반환해.\n");
        prompt.append("형식: { \"text\": \"정답 텍스트\" }\n\n");
        prompt.append("문제: ").append(questionText).append("\n\n");
        prompt.append("보기:\n");
        for (Map<String, String> option : options) {
            prompt.append("- ").append(option.get("text")).append("\n");
        }
        prompt.append("\n※ 보기 번호(id)는 주어지지 않으니 절대 사용하지 마. 보기 중 하나의 텍스트만 JSON으로 반환해.");

        String gptResponse = getGptResponse(prompt.toString());
        System.out.println("GPT 응답 원문: " + gptResponse);

        try {
            Map<String, String> parsed = new ObjectMapper().readValue(gptResponse, new TypeReference<>() {});
            String returnedText = parsed.get("text");

            for (Map<String, String> option : options) {
                if (option.get("text").trim().equals(returnedText.trim())) {
                    return Map.of("id", option.get("id"), "text", option.get("text"));
                }
            }

            System.out.println("⚠️ GPT가 준 text와 일치하는 보기 없음: " + returnedText);
            return null;

        } catch (Exception e) {
            System.out.println("❌ 응답 파싱 실패: " + e.getMessage());
            return null;
        }
    }




}
