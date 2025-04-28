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

            String explanation = (String) problem.get("explanation");
            if (explanation == null) {
                iterator.remove();
                continue;
            }

            String extractedAnswer = extractAnswerFromExplanation(explanation);
            if (extractedAnswer == null) {
                iterator.remove();
                continue;
            }

            if ("multiple".equals(type)) {
                problem.put("answer", extractedAnswer); // 그대로 string 저장
                fixDuplicateCorrectOptions(problem, extractedAnswer);
            } else if ("truefalse".equals(type)) {
                boolean boolAnswer = "true".equalsIgnoreCase(extractedAnswer.trim());
                problem.put("answer", boolAnswer); // boolean 저장
            }
        }

        // 문제 수 맞추기
        if (problems.size() > expectedCount) {
            problems = problems.subList(0, expectedCount);
        }

        return mapper.writeValueAsString(problems);
    }

    private static String extractAnswerFromExplanation(String explanation) {
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

    private static void fixDuplicateCorrectOptions(Map<String, Object> problem, String correctId) {
        List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
        if (options == null || options.isEmpty()) return;

        boolean firstFound = false;
        for (Map<String, String> opt : options) {
            if (correctId.equals(opt.get("id"))) {
                if (!firstFound) {
                    firstFound = true;
                } else {
                    opt.put("text", opt.get("text") + "(오답)");
                }
            }
        }
    }
}
