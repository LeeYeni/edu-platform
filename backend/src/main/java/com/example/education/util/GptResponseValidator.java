package com.example.education.util;

import com.example.education.service.GptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Map<String, Map<String, Object>> problemMap = new HashMap<>();
        for (Map<String, Object> problem : problems) {
            String questionId = (String) problem.get("question_id");
            Integer questionNum = (Integer) problem.get("question_num");
            if (questionId != null && questionNum != null) {
                problemMap.put(makeKey(questionId, questionNum), problem);
            }
        }

        for (Map<String, Object> problem : problems) {
            String type = (String) problem.get("type");
            log.info("💡 문제 유형: {}", type);
            if (type == null) continue;

            String questionId = (String) problem.get("question_id");
            Integer questionNum = (Integer) problem.get("question_num");
            if (questionId == null || questionNum == null) continue;

            Map<String, Object> targetProblem = problemMap.get(makeKey(questionId, questionNum));
            if (targetProblem == null) continue;

            if ("multiple".equals(type)) {
                fixMultipleAnswer(targetProblem);
            } else if ("truefalse".equals(type)) {
                fixTrueFalseAnswer(targetProblem);
            }
        }

        if (problems.size() > expectedCount) {
            problems = problems.subList(0, expectedCount);
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
        try {
            if (explanation == null) return null;
            int idx = explanation.indexOf("정답은 ");
            if (idx == -1) return null;

            String sub = explanation.substring(idx + 5).trim();
            if (sub.contains("입니다")) {
                sub = sub.substring(0, sub.indexOf("입니다")).trim();
            }

            return sub.replaceAll("[^a-zA-Z]", "").toLowerCase();
        } catch (Exception e) {
            return null;
        }
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
