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

    public String solveProblemAndExtractAnswer(String questionText, List<Map<String, String>> options) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 초등학교 수학 문제를 풀고, 정답이 되는 보기의 id (a, b, c, d)만 답변해줘.\n\n");
        prompt.append("문제: ").append(questionText).append("\n\n");
        prompt.append("선택지:\n");
        for (Map<String, String> option : options) {
            String id = option.get("id");
            String text = option.get("text");
            prompt.append(id).append(". ").append(text).append("\n");
        }
        prompt.append("\n정답을 '정답은 ~입니다.' 형식으로 답해줘. 이때, 정답은 id인 a, b, c, d 중에 하나여야 해. 다른 말은 하지 말고.");

        // 실제 GPT 호출
        String gptResponse = getGptResponse(prompt.toString());

        // "정답은 a입니다." 같은 결과에서 'a' 추출
        return extractAnswerIdFromGptResponse(gptResponse);
    }

    private String extractAnswerIdFromGptResponse(String gptResponse) {
        try {
            int idx = gptResponse.indexOf("정답은 ");
            if (idx == -1) return null;
            String sub = gptResponse.substring(idx + 5).trim();
            if (sub.contains("입니다")) {
                sub = sub.substring(0, sub.indexOf("입니다")).trim();
            }
            return sub.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }
}