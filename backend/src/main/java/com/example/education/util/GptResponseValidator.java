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
            Map<String, String> solvedResult = gptService.solveProblemAndExtractAnswer(questionText, options);
            if (solvedResult == null) return;

            String solvedText = solvedResult.get("text").trim();

            // GPT가 준 text와 일치하는 id 찾기
            String matchedId = null;
            for (Map<String, String> option : options) {
                if (option.get("text").trim().equals(solvedText)) {
                    matchedId = option.get("id");
                    break;
                }
            }

            // 일치하는 text가 없다면 랜덤으로 하나 골라 해당 보기의 text를 수정
            if (matchedId == null) {
                List<String> ids = options.stream().map(opt -> opt.get("id")).toList();
                String randomId = ids.get(new Random().nextInt(ids.size()));
                for (Map<String, String> option : options) {
                    if (option.get("id").equals(randomId)) {
                        option.put("text", solvedText);  // 보기 교체
                        matchedId = randomId;
                        log.info("⚠️ 보기 없음 → {}번 보기를 '{}'로 수정", matchedId, solvedText);
                        break;
                    }
                }
            }

            // 정답 업데이트
            if (matchedId != null && !matchedId.equalsIgnoreCase(originalAnswer)) {
                log.info("⚠️ 정답 수정: {} → {}", originalAnswer, matchedId);
                problem.put("answer", matchedId);
            }

            updateExplanationAnswer(problem, matchedId, solvedText);

        } catch (Exception e) {
            log.warn("[GPT 재풀이 실패] {}", e.getMessage());
        }
    }

    private static void fixTrueFalseAnswer(Map<String, Object> problem) {
        String explanation = (String) problem.get("explanation");
        if (explanation == null) return;

        // 1. 정답 true/false 추출 (GPT 해설 기반)
        Pattern pattern = Pattern.compile("정답은\\s*(true|false)\\s*입니다[.]?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(explanation.toLowerCase());

        boolean isAnswerTrue = false; // 기본값 false
        if (matcher.find()) {
            String result = matcher.group(1).toLowerCase();
            isAnswerTrue = Boolean.parseBoolean(result);
            problem.put("answer", isAnswerTrue);
        }

        // 2. 해설 정리
        explanation = cleanRepeatedExplanation(explanation); // 중복 문장 제거
        explanation = cleanContradictionInExplanation(explanation, isAnswerTrue); // true/false에 따라 마무리

        problem.put("explanation", explanation);
    }





    public static String extractAnswerIdFromExplanation(String explanation) {
        if (explanation == null) return "";
        Pattern pattern = Pattern.compile("정답은\\s*\\(?([a-dA-D])\\)?\\s*입니다");
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

    private static void updateExplanationAnswer(Map<String, Object> problem, String correctId, String correctText) {
        String explanation = (String) problem.get("explanation");

        if (explanation != null) {
            // "정답은 ~입니다." 패턴을 찾아서 정답 부분만 바꾸기
            Pattern pattern = Pattern.compile("정답은\\s*([a-dA-D])(\\s*\\([^)]*\\))?\\s*입니다\\.");
            Matcher matcher = pattern.matcher(explanation);

            if (matcher.find()) {
                // 기존 정답 문장을 새 정답 문장으로 교체
                String updated = matcher.replaceFirst("정답은 " + correctId + " (" + correctText + ")입니다.");
                problem.put("explanation", updated);
                return;
            }
        }

        // 기존 정답 문장이 없을 경우, 메시지 없이 깔끔하게 추가
        problem.put("explanation", (explanation != null ? explanation.trim() + " " : "") +
                "정답은 " + correctId + " (" + correctText + ")입니다.");
    }

    private static String cleanRepeatedExplanation(String explanation) {
        // 같은 문장이 여러 번 반복되는 경우 첫 문장만 남긴다
        String[] sentences = explanation.split("\\.\\s*");
        LinkedHashSet<String> uniqueSentences = new LinkedHashSet<>();

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (!sentence.isEmpty()) {
                uniqueSentences.add(sentence);
            }
        }

        return String.join(". ", uniqueSentences) + ".";
    }


    private static String cleanContradictionInExplanation(String explanation, boolean isAnswerTrue) {
        if (explanation == null) return "";

        // 1. "가 아니라 ~입니다" 같은 혼란스러운 문장을 제거
        explanation = explanation.replaceAll("가 아니라 [^\\.]+", "");

        // 2. "같은 문장 반복" 현상은 따로 cleanRepeatedExplanation()에서 처리한다고 가정

        // 3. 끝 문장 통일 (true이면 "입니다.", false이면 "아닙니다.")
        if (isAnswerTrue) {
            if (!explanation.trim().endsWith("입니다.")) {
                explanation = explanation.trim();
                if (explanation.endsWith(".")) {
                    explanation = explanation.substring(0, explanation.length() - 1);
                }
                explanation += "입니다.";
            }
        } else {
            if (!explanation.trim().endsWith("아닙니다.")) {
                explanation = explanation.trim();
                if (explanation.endsWith(".")) {
                    explanation = explanation.substring(0, explanation.length() - 1);
                }
                explanation += "아닙니다.";
            }
        }

        return explanation;
    }
}
