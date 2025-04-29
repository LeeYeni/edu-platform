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
        prompt.append("\n정답을 '정답은 ~입니다.' 형식으로 답변해줘. ");
        prompt.append("반드시 id(a, b, c, d) 중 하나만 답변해. 다른 말은 하지 마.");

        // GPT 호출
        String gptResponse = getGptResponse(prompt.toString());

        System.out.println("gpt가 풀어준 답 원문: " + gptResponse);

        // "정답은 ~입니다." 포맷에서 정답 id 추출
        return extractAnswerIdFromExplanation(gptResponse);
    }
}
