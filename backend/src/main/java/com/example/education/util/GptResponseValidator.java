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

        try {
            // GPT에게 문제 + 보기 전체를 보내서 다시 풀게 한다
            String solvedAnswer = gptService.solveProblemAndExtractAnswer(questionText, options);
            System.out.println("[검증] 문제: " + questionText);
            System.out.println("[검증] 기존 정답: " + originalAnswer + ", GPT 풀이 정답: " + solvedAnswer);

            if (solvedAnswer != null && !solvedAnswer.equalsIgnoreCase(originalAnswer)) {
                // 정답이 다르면, 문제 객체를 수정한다
                problem.put("answer", solvedAnswer);
                enforceSingleCorrectOption(options, solvedAnswer);
                updateExplanationAnswer(problem, solvedAnswer);
            }
        } catch (Exception e) {
            // 실패해도 무시하고 기존 answer를 유지한다
            System.err.println("[Warning] GPT 재풀이 실패: " + e.getMessage());
        }

    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem) {
        String explanation = (String) problem.get("explanation");
        if (explanation == null) return;
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

    private static void updateExplanationAnswer(Map<String, Object> problem, String correctId) {
        String newExplanation = "따라서 정답은 " + correctId + "입니다.";
        problem.put("explanation", newExplanation);
    }

    private static String cleanExplanation(String explanation) {
        if (explanation.contains("아니라")) {
            return explanation.substring(explanation.indexOf("아니라") + 3).trim();
        }
        return explanation.trim();
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
