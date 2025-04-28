package com.example.education.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GptResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String validateAndClean(String rawResponse, int expectedCount) throws Exception {
        List<Map<String, Object>> problems = mapper.readValue(rawResponse, new TypeReference<>() {});

        // problems를 (question_id, question_num) 기준으로 찾기 쉽게 Map으로 변환
        Map<String, Map<String, Object>> problemMap = new HashMap<>();
        for (Map<String, Object> problem : problems) {
            String questionId = (String) problem.get("question_id");
            Integer questionNum = (Integer) problem.get("question_num");

            if (questionId != null && questionNum != null) {
                problemMap.put(makeKey(questionId, questionNum), problem);
            }
        }

        // 각 문제를 수정
        for (Map<String, Object> problem : problems) {
            String type = (String) problem.get("type");
            if (type == null) continue;

            String questionId = (String) problem.get("question_id");
            Integer questionNum = (Integer) problem.get("question_num");
            if (questionId == null || questionNum == null) continue;

            Map<String, Object> targetProblem = problemMap.get(makeKey(questionId, questionNum));
            if (targetProblem == null) continue; // 매칭되는 문제가 없으면 스킵

            String explanation = (String) targetProblem.get("explanation");
            if (explanation == null) continue;

            String extractedAnswerId = extractAnswerIdFromExplanation(explanation);
            if (extractedAnswerId == null) continue;

            if ("multiple".equals(type)) {
                fixMultipleAnswer(targetProblem, extractedAnswerId, explanation);
            } else if ("truefalse".equals(type)) {
                fixTrueFalseAnswer(targetProblem, explanation);
            }
        }

        // 문제 수 맞추기
        if (problems.size() > expectedCount) {
            problems = problems.subList(0, expectedCount);
        }

        return mapper.writeValueAsString(problems);
    }

    private static String makeKey(String questionId, Integer questionNum) {
        return questionId + "-" + questionNum;
    }

    private static String extractAnswerIdFromExplanation(String explanation) {
        try {
            int idx = explanation.indexOf("정답은 ");
            if (idx == -1) return null;
            String sub = explanation.substring(idx + 5).trim();
            if (sub.contains("입니다")) {
                sub = sub.substring(0, sub.indexOf("입니다")).trim();
            }
            return sub.toLowerCase(); // 항상 소문자로 변환 (a/b/c/d, true/false)
        } catch (Exception e) {
            return null;
        }
    }

    private static void fixMultipleAnswer(Map<String, Object> problem, String extractedId, String explanation) {
        List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
        if (options == null || options.isEmpty()) return;

        String extractedText = options.stream()
                .filter(opt -> extractedId.equals(opt.get("id")))
                .map(opt -> opt.get("text"))
                .findFirst()
                .orElse(null);

        if (extractedText == null) {
            // extractedId가 options에 없으면 a로 fallback
            problem.put("answer", "a");
            return;
        }

        String expectedMeaning = extractMeaningFromExplanation(explanation);
        if (expectedMeaning == null) {
            problem.put("answer", extractedId);
            return;
        }

        if (meaningMatches(extractedText, expectedMeaning)) {
            problem.put("answer", extractedId);
        } else {
            Optional<String> matchedId = options.stream()
                    .filter(opt -> meaningMatches(opt.get("text"), expectedMeaning))
                    .map(opt -> opt.get("id"))
                    .findFirst();

            problem.put("answer", matchedId.orElse("a"));
        }
    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem, String explanation) {
        explanation = cleanExplanation(explanation);

        String expectedMeaning = extractMeaningFromExplanation(explanation);
        if (expectedMeaning == null) return;

        boolean answer = expectedMeaning.toLowerCase().contains("true");
        problem.put("answer", answer);
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

    private static String cleanExplanation(String explanation) {
        if (explanation.contains("아니라")) {
            return explanation.substring(explanation.indexOf("아니라") + 3).trim();
        }
        return explanation.trim();
    }

    private static boolean meaningMatches(String text, String expectedMeaning) {
        String normalizedText = normalize(text);
        String normalizedExpected = normalize(expectedMeaning);
        return normalizedText.equals(normalizedExpected);
    }

    private static String normalize(String input) {
        return input.replaceAll("[^0-9가-힣a-zA-Z]", "").toLowerCase();
    }
}
