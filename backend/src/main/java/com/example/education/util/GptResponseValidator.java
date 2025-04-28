package com.example.education.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GptResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String validateAndClean(String rawResponse, int expectedCount) throws Exception {
        List<Map<String, Object>> problems = mapper.readValue(rawResponse, new TypeReference<>() {});

        for (Map<String, Object> problem : problems) {
            String type = (String) problem.get("type");
            if (type == null) continue;

            String explanation = (String) problem.get("explanation");
            if (explanation == null) continue;

            String extractedAnswerId = extractAnswerIdFromExplanation(explanation);
            if (extractedAnswerId == null) continue;

            if ("multiple".equals(type)) {
                fixMultipleAnswer(problem, extractedAnswerId, explanation);
            } else if ("truefalse".equals(type)) {
                fixTrueFalseAnswer(problem, explanation);
            }
        }

        if (problems.size() > expectedCount) {
            problems = problems.subList(0, expectedCount);
        }

        return mapper.writeValueAsString(problems);
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

        // extractedId가 진짜 올바른지 검증: extractedId → options 리스트에서 id 매칭해서 text 가져오기
        String extractedText = options.stream()
                .filter(opt -> extractedId.equals(opt.get("id")))
                .map(opt -> opt.get("text"))
                .findFirst()
                .orElse(null);

        if (extractedText == null) {
            // extractedId가 options에 없는 경우 → a 선택
            problem.put("answer", "a");
            return;
        }

        // 해설에서 실제 정답 의미를 추출
        String expectedMeaning = extractMeaningFromExplanation(explanation);

        if (expectedMeaning == null) {
            // 해설에서 의미를 뽑지 못하면 fallback으로 그냥 extractedId
            problem.put("answer", extractedId);
            return;
        }

        // extractedText와 expectedMeaning 비교
        if (meaningMatches(extractedText, expectedMeaning)) {
            // 의미가 일치하면 extractedId를 answer로
            problem.put("answer", extractedId);
        } else {
            // 의미가 일치하지 않으면 options 중에서 meaning과 일치하는 text를 찾아서 answer를 재설정
            Optional<String> matchedId = options.stream()
                    .filter(opt -> meaningMatches(opt.get("text"), expectedMeaning))
                    .map(opt -> opt.get("id"))
                    .findFirst();

            if (matchedId.isPresent()) {
                problem.put("answer", matchedId.get());
            } else {
                // 그래도 없으면 a로 fallback
                problem.put("answer", "a");
            }
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
            // "348 + 129 = 477입니다." 같은 핵심 서술을 잘라낸다
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
        // 숫자 계산 결과 비교 우선
        String normalizedText = normalize(text);
        String normalizedExpected = normalize(expectedMeaning);
        return normalizedText.equals(normalizedExpected);
    }

    private static String normalize(String input) {
        return input.replaceAll("[^0-9가-힣a-zA-Z]", "").toLowerCase();
    }
}
