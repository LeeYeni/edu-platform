package com.example.education.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GptResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String validateAndClean(String rawResponse, int expectedCount) throws Exception {
        List<Map<String, Object>> problems = mapper.readValue(rawResponse, new TypeReference<>() {});

        Iterator<Map<String, Object>> iterator = problems.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> problem = iterator.next();

            String type = (String) problem.get("type");
            if (type == null) {
                iterator.remove();
                continue;
            }

            if ("multiple".equals(type)) {
                fixMultipleAnswer(problem);
            } else if ("truefalse".equals(type)) {
                fixTrueFalseAnswer(problem);
            }
        }

        // 필요시 문제 수 조정
        if (problems.size() > expectedCount) {
            problems = problems.subList(0, expectedCount);
        }

        return mapper.writeValueAsString(problems);
    }

    private static void fixMultipleAnswer(Map<String, Object> problem) {
        List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
        if (options == null || options.isEmpty()) return;

        String explanation = (String) problem.get("explanation");
        if (explanation == null) return;

        String correctText = extractCorrectTextFromExplanation(explanation);
        if (correctText == null) return;

        Optional<Map<String, String>> matchedOption = options.stream()
                .filter(opt -> correctText.equals(opt.get("text")))
                .findFirst();

        if (matchedOption.isPresent()) {
            problem.put("answer", matchedOption.get().get("id"));
        } else {
            // 일치하는 option 없으면 첫 번째 걸 정답으로 설정
            problem.put("answer", options.get(0).get("id"));
        }

        fixDuplicateCorrectOptions(options, correctText);
    }

    private static void fixDuplicateCorrectOptions(List<Map<String, String>> options, String correctText) {
        boolean firstFound = false;
        for (Map<String, String> opt : options) {
            if (correctText.equals(opt.get("text"))) {
                if (!firstFound) {
                    firstFound = true;
                } else {
                    opt.put("text", opt.get("text") + "(오답)");
                }
            }
        }
    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem) {
        String text = (String) problem.get("text");
        String explanation = (String) problem.get("explanation");
        if (text == null || explanation == null) return;

        String cleanedExplanation = cleanExplanation(explanation);
        String trueStatement = extractTrueStatementFromExplanation(cleanedExplanation);
        if (trueStatement == null) return;

        boolean matches = normalize(text).equals(normalize(trueStatement));

        problem.put("answer", matches);
        problem.put("explanation", trueStatement);
    }

    private static String extractCorrectTextFromExplanation(String explanation) {
        try {
            int idx = explanation.indexOf("정답은 ");
            if (idx == -1) return null;
            String sub = explanation.substring(idx + 5).trim();
            if (sub.contains("입니다")) {
                sub = sub.substring(0, sub.indexOf("입니다")).trim();
            }
            return sub;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractTrueStatementFromExplanation(String explanation) {
        try {
            if (explanation.contains("입니다")) {
                return explanation.substring(0, explanation.indexOf("입니다") + 3).trim();
            }
            return null;
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

    private static String normalize(String input) {
        return input.replaceAll("\\s", "").replace("=", "").replace("는", "").replace("이다", "").replace("입니다", "").trim();
    }
}
