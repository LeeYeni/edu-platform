package com.example.education.util;

import com.example.education.service.GptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GptResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static GptService gptService; // GptService 주입 받음

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

            String extractedAnswerId = extractAnswerIdFromExplanation(explanation);
            if (extractedAnswerId == null) continue;

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

    private static void fixMultipleAnswer(Map<String, Object> problem, String explanation) {
        List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
        if (options == null || options.isEmpty()) return;

        try {
            String finalAnswer;

            if (isNumericOptions(options)) {
                String calculatedResult = extractCalculatedResult(explanation);
                if (calculatedResult != null) {
                    finalAnswer = options.stream()
                            .filter(opt -> normalize(opt.get("text")).equals(normalize(calculatedResult)))
                            .map(opt -> opt.get("id"))
                            .findFirst()
                            .orElse("a");
                } else {
                    finalAnswer = "a";
                }
            } else {
                finalAnswer = callGptForMeaningMatch(explanation, options);
            }

            problem.put("answer", finalAnswer);
            enforceSingleCorrectOption(options, finalAnswer);

        } catch (Exception e) {
            problem.put("answer", "a");
            enforceSingleCorrectOption(options, "a");
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

    private static boolean isNumericOptions(List<Map<String, String>> options) {
        for (Map<String, String> option : options) {
            String text = option.get("text");
            if (text == null || !text.matches("[0-9]+")) {
                return false;
            }
        }
        return true;
    }

    private static String extractCalculatedResult(String explanation) {
        try {
            if (explanation.contains("=")) {
                String[] parts = explanation.split("=");
                if (parts.length > 1) {
                    return parts[1].replaceAll("[^0-9]", "").trim();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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

    private static boolean meaningMatches(String text, String expectedMeaning) {
        String normalizedText = normalize(text);
        String normalizedExpected = normalize(expectedMeaning);

        if (normalizedText.matches("\\d+") && normalizedExpected.matches("\\d+")) {
            return normalizedText.equals(normalizedExpected);
        }

        return normalizedText.equals(normalizedExpected);
    }

    private static String normalize(String input) {
        return input.replaceAll("[^0-9가-힣a-zA-Z]", "").toLowerCase();
    }

    private static String callGptForMeaningMatch(String explanation, List<Map<String, String>> options) throws Exception {
        if (gptService == null) {
            throw new IllegalStateException("GptService가 설정되지 않았습니다.");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("너는 문제를 검토하고 해설을 이해하여, 보기 중 해설과 의미적으로 가장 일치하는 정답 id를 판단하는 역할을 합니다.\n")
                .append("- 반드시 id (a, b, c, d) 중 하나만 골라야 합니다.\n")
                .append("- 복수 정답을 허용하지 않습니다. 하나만 고르세요.\n\n")
                .append("[해설]\n").append(explanation).append("\n\n")
                .append("[보기 목록]\n");

        for (Map<String, String> opt : options) {
            prompt.append(opt.get("id")).append(". ").append(opt.get("text")).append("\n");
        }

        prompt.append("\n질문: 위 해설 의미와 가장 일치하는 보기의 id를 하나만 골라주세요. 답변은 id(a/b/c/d)만 말해주세요.");

        String gptResponse = gptService.getGptResponse(prompt.toString());
        return extractAnswerIdFromGptResponse(gptResponse);
    }

    private static String extractAnswerIdFromGptResponse(String response) {
        response = response.trim().toLowerCase();
        if (response.contains("a")) return "a";
        if (response.contains("b")) return "b";
        if (response.contains("c")) return "c";
        if (response.contains("d")) return "d";
        return "a"; // fallback
    }
}
