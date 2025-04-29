package com.example.education.util;

import com.example.education.service.GptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GptResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static GptService gptService;

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
            if (type == null) continue;

            String questionId = (String) problem.get("question_id");
            Integer questionNum = (Integer) problem.get("question_num");
            if (questionId == null || questionNum == null) continue;

            Map<String, Object> targetProblem = problemMap.get(makeKey(questionId, questionNum));
            if (targetProblem == null) continue;

            String explanation = (String) targetProblem.get("explanation");
            if (explanation == null) continue;

            if ("multiple".equals(type)) {
                fixMultipleAnswer(targetProblem, explanation);
            } else if ("truefalse".equals(type)) {
                fixTrueFalseAnswer(targetProblem, explanation);
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

    private static void fixMultipleAnswer(Map<String, Object> problem, String explanation) {
        List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
        if (options == null || options.isEmpty()) return;

        String extractedId = extractAnswerIdFromExplanation(explanation);
        if (extractedId == null) return;

        String extractedText = options.stream()
                .filter(opt -> extractedId.equals(opt.get("id")))
                .map(opt -> opt.get("text"))
                .findFirst()
                .orElse(null);

        String expectedMeaning = extractMeaningFromExplanation(explanation);
        if (expectedMeaning == null || extractedText == null) {
            problem.put("answer", "a");
            enforceSingleCorrectOption(options, "a");
            return;
        }

        if (meaningMatches(extractedText, expectedMeaning)) {
            problem.put("answer", extractedId);
            enforceSingleCorrectOption(options, extractedId);
            return;
        }

        Optional<String> matchedId = options.stream()
                .filter(opt -> meaningMatches(opt.get("text"), expectedMeaning))
                .map(opt -> opt.get("id"))
                .findFirst();

        String finalId = matchedId.orElse("a");
        problem.put("answer", finalId);
        enforceSingleCorrectOption(options, finalId);
        updateExplanationAnswer(problem, explanation, finalId);
    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem, String explanation) {
        explanation = cleanExplanation(explanation);
        String expectedMeaning = extractMeaningFromExplanation(explanation);
        if (expectedMeaning == null) return;
        boolean answer = expectedMeaning.toLowerCase().contains("true");
        problem.put("answer", answer);
    }

    private static String extractAnswerIdFromExplanation(String explanation) {
        try {
            int idx = explanation.indexOf("정답은 ");
            if (idx == -1) return null;
            String sub = explanation.substring(idx + 5).trim();
            if (sub.contains("입니다")) {
                sub = sub.substring(0, sub.indexOf("입니다")).trim();
            }
            return sub.toLowerCase();
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

    private static void updateExplanationAnswer(Map<String, Object> problem, String explanation, String correctId) {
        String newExplanation;
        if (explanation.contains("정답은 ")) {
            int idx = explanation.indexOf("정답은 ");
            String before = explanation.substring(0, idx);
            newExplanation = before + "정답은 " + correctId + "입니다.";
        } else {
            newExplanation = explanation + " 따라서 정답은 " + correctId + "입니다.";
        }
        problem.put("explanation", newExplanation);
    }

    private static String cleanExplanation(String explanation) {
        if (explanation.contains("아니라")) {
            return explanation.substring(explanation.indexOf("아니라") + 3).trim();
        }
        return explanation.trim();
    }

    private static boolean meaningMatches(String text, String expectedMeaning) {
        return normalize(text).equals(normalize(expectedMeaning));
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

    private static String normalize(String input) {
        return input.replaceAll("[^0-9가-힣a-zA-Z]", "").toLowerCase();
    }
}
