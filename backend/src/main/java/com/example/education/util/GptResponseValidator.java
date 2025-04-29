package com.example.education.util;

import com.example.education.service.GptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.*;

public class GptResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static GptService gptService;
    private static final Logger log = LoggerFactory.getLogger(GptResponseValidator.class);

    public static void setGptService(GptService service) {
        gptService = service;
    }

    public static String validateAndClean(String rawResponse, int expectedCount) throws Exception {
        List<Map<String, Object>> problems = mapper.readValue(rawResponse, new TypeReference<>() {});

        // ✅ 문제 수 초과 시 잘라내기
        if (problems.size() > expectedCount) {
            problems = problems.subList(0, expectedCount);
        }

        // ✅ 각 문제에 임시 question_id, question_num 부여
        for (int i = 0; i < problems.size(); i++) {
            problems.get(i).put("question_id", "v-temp");
            problems.get(i).put("question_num", i + 1);
        }

        // ✅ 문제별 정답 검증 및 수정
        for (Map<String, Object> problem : problems) {
            String type = (String) problem.get("type");
            if (type == null) continue;

            switch (type) {
                case "multiple" -> fixMultipleAnswer(problem);
                case "truefalse" -> fixTrueFalseAnswer(problem);
                // subjective는 별도 처리 불필요
            }
        }

        return mapper.writeValueAsString(problems);
    }


    private static String makeKey(String questionId, Integer questionNum) {
        return questionId + "-" + questionNum;
    }

    private static void fixMultipleAnswer(Map<String, Object> problem) {
        String questionText = (String) problem.get("text");
        List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
        if (questionText == null || options == null || options.isEmpty()) return;

        String originalAnswer = (String) problem.get("answer");

        log.info("🔍 fixMultipleAnswer 진입: 문제 = {}", questionText);
        log.info("🔍 기존 정답: {}", originalAnswer);

        try {
            String solvedAnswer = gptService.solveProblemAndExtractAnswer(questionText, options);
            log.info("✅ GPT 풀이 정답: {}", solvedAnswer);

            if (solvedAnswer != null && !solvedAnswer.equalsIgnoreCase(originalAnswer)) {
                log.info("⚠️ 정답 수정: {} → {}", originalAnswer, solvedAnswer);
                problem.put("answer", solvedAnswer);
                enforceSingleCorrectOption(options, solvedAnswer);
                updateExplanationAnswer(problem, solvedAnswer);
            }
        } catch (Exception e) {
            log.warn("[GPT 재풀이 실패] {}", e.getMessage());
        }
    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem) {
        String explanation = (String) problem.get("explanation");
        if (explanation == null) return;

        String expectedMeaning = extractMeaningFromExplanation(explanation);
        if (expectedMeaning == null) return;
        boolean answer = expectedMeaning.toLowerCase().contains("true");
        problem.put("answer", answer);
    }

    public static String extractAnswerIdFromExplanation(String explanation) {
        if (explanation == null) return "";

        Pattern pattern = Pattern.compile("정답은\\s*([a-dA-D])\\s*입니다");
        Matcher matcher = pattern.matcher(explanation);

        if (matcher.find()) {
            return matcher.group(1).toLowerCase();  // 항상 소문자 반환
        }

        return "";
    }

    private static String extractMeaningFromExplanation(String explanation) {
        try {
            if (explanation.contains("입니다")) {
                return explanation.substring(0, explanation.indexOf("입니다")).trim();
            }
            return explanation.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static void updateExplanationAnswer(Map<String, Object> problem, String correctId) {
        problem.put("explanation", "따라서 정답은 " + correctId + "입니다.");
    }

    private static void enforceSingleCorrectOption(List<Map<String, String>> options, String correctId) {
        for (Map<String, String> option : options) {
            String id = option.get("id");
            if (!correctId.equals(id)) {
                String text = option.get("text");
                if (text != null && !text.contains("(오답)")) {
                    option.put("text", text + "(오답)");
                }
            }
        }
    }
}
