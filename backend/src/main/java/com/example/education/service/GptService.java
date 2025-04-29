package com.example.education.service;

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

    public String solveProblemAndExtractAnswer(String questionText, List<Map<String, String>> options) {
        // 보기 text 정리 (예: (오답) 제거)
        for (Map<String, String> option : options) {
            String text = option.get("text");
            if (text != null) {
                option.put("text", text.replaceAll("\\(오답\\)", "").trim());
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 보기 중 올바른 정답을 찾아 해당 보기의 id를 정확히 선택해줘.\n\n");
        prompt.append("문제: ").append(questionText).append("\n\n");
        prompt.append("보기:\n");

        for (Map<String, String> option : options) {
            String id = option.get("id");
            String text = option.get("text");
            prompt.append(id).append(". ").append(text).append("\n");
        }

        prompt.append("\n정답을 id(a, b, c, d) 중 하나로만 답해. 반드시 보기의 text와 정확히 일치하는 id를 선택해.\n");
        prompt.append("정답 형식: 정답은 (id)입니다.");

        // GPT 호출
        String gptResponse = getGptResponse(prompt.toString());

        System.out.println("gpt가 풀어준 답 원문: " + gptResponse);

        // "정답은 ~입니다." 포맷에서 정답 id 추출
        return extractAnswerIdFromExplanation(gptResponse);
    }

}
