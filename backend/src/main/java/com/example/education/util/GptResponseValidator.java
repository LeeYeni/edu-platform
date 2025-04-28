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

        // 필요하면 개수 조정
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

        // "정답은 ~" 형태 파싱
        String correctText = extractCorrectTextFromExplanation(explanation);
        if (correctText == null) return;

        // options 중 matching
        Optional<Map<String, String>> matchedOption = options.stream()
                .filter(opt -> correctText.equals(opt.get("text")))
                .findFirst();

        if (matchedOption.isPresent()) {
            problem.put("answer", matchedOption.get().get("id"));
        } else {
            // 일치하는 option이 없으면 가장 첫 번째(a, b, c, d 순)를 answer로 설정
            problem.put("answer", options.get(0).get("id"));
        }

        // 복수 정답 정리: 동일 text 옵션 여러 개 있으면 하나만 남기고 나머지 오답으로 살짝 수정
        fixDuplicateCorrectOptions(options, correctText);
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

    private static void fixDuplicateCorrectOptions(List<Map<String, String>> options, String correctText) {
        boolean firstFound = false;
        for (Map<String, String> opt : options) {
            if (correctText.equals(opt.get("text"))) {
                if (!firstFound) {
                    firstFound = true;
                } else {
                    // 같은 정답 텍스트가 여러 개 있을 때: 오답으로 변경
                    opt.put("text", opt.get("text") + "(수정)");
                }
            }
        }
    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem) {
        String text = (String) problem.get("text");
        String explanation = (String) problem.get("explanation");
        if (text == null || explanation == null) return;

        // 해설에서 "~입니다" 문장 추출
        String trueStatement = extractTrueStatementFromExplanation(explanation);
        if (trueStatement == null) return;

        // 문제 지문과 해설 명제 비교
        boolean matches = normalize(text).equals(normalize(trueStatement));

        problem.put("answer", matches);

        // 해설 깔끔하게 정리 (긍정 표현만 남기기)
        problem.put("explanation", trueStatement);
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

    private static String normalize(String input) {
        return input.replaceAll("\\s", "").replace("=", "").replace("는", "").replace("이다", "").replace("입니다", "").trim();
    }
}
