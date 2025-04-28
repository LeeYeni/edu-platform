package com.example.education.service;

import com.example.education.entity.Question;
import com.example.education.repository.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public String saveQuestionsFromGptResponse(String userId, String userType,
                                               String unit1, String unit2, String unit3,
                                               String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        List<Question> savedQuestions = new ArrayList<>();

        boolean isTeacher = !"student".equals(userType);
        String prefix = isTeacher ? "t" : "s";

        // 기존 문제 묶음 수(count of distinct question_id)
        long existingBatchCount = questionRepository.countDistinctQuestionIdByUserId(userId);
        long batchIndex = existingBatchCount + 1;
        String questionId = String.format("%s-%s-%d", prefix, userId, batchIndex);  // e.g. s-8591030-3

        if (root.isArray()) {
            int index = 1;
            for (JsonNode node : root) {
                Question q = parseQuestionNode(node, questionId, userId, unit1, unit2, unit3, index++);
                savedQuestions.add(questionRepository.save(q));
            }
        } else {
            Question q = parseQuestionNode(root, questionId, userId, unit1, unit2, unit3, 1);
            savedQuestions.add(questionRepository.save(q));
        }

        return questionId;
    }

    private Question parseQuestionNode(JsonNode node, String questionId, String userId,
                                       String unit1, String unit2, String unit3, int number) {
        String type = node.get("type").asText();
        String text = node.get("text").asText();
        String answer = node.get("answer").toString(); // boolean/string 대응
        String explanation = node.get("explanation").asText();
        String options = node.has("options") ? node.get("options").toString() : null;

        return Question.builder()
                .questionId(questionId)
                .userId(userId)
                .questionNum(number)
                .questionType(type)
                .questionText(text)
                .options(options)
                .answer(answer)
                .explanation(explanation)
                .unit1(unit1)
                .unit2(unit2)
                .unit3(unit3)
                .build();
    }

    public String validateAndFixGptResponse(String rawResponse, int expectedNumProblems) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> problems = mapper.readValue(rawResponse, new TypeReference<List<Map<String, Object>>>() {});

            // ✅ 1. 문제 개수 체크
            if (problems.size() != expectedNumProblems) {
                throw new RuntimeException("문제 개수가 요청한 개수와 다릅니다.");
            }

            // ✅ 2. 각 문제 검증
            for (Map<String, Object> problem : problems) {
                String type = (String) problem.get("type");
                if ("multiple".equals(type)) {
                    List<Map<String, String>> options = (List<Map<String, String>>) problem.get("options");
                    if (options == null || options.size() < 2) {
                        throw new RuntimeException("객관식인데 보기(options)가 부족합니다.");
                    }
                    String answer = (String) problem.get("answer");
                    boolean valid = options.stream().anyMatch(opt -> opt.get("id").equals(answer));
                    if (!valid) {
                        throw new RuntimeException("객관식 answer가 options에 없습니다.");
                    }
                }

                if ("truefalse".equals(type)) {
                    Object answer = problem.get("answer");
                    if (!(answer instanceof Boolean)) {
                        throw new RuntimeException("truefalse 문제의 answer는 boolean이어야 합니다.");
                    }
                }

                if ("subjective".equals(type)) {
                    Object answer = problem.get("answer");
                    if (!(answer instanceof String)) {
                        throw new RuntimeException("subjective 문제의 answer는 문자열이어야 합니다.");
                    }
                }

                // ✅ 3. explanation과 answer 일치 검증
                String explanation = (String) problem.get("explanation");
                if (explanation != null && explanation.contains("아닙니다")) {
                    throw new RuntimeException("부정형 해설 문장이 감지되었습니다.");
                }
            }

            // 모두 통과했으면 다시 JSON 문자열로 반환
            return mapper.writeValueAsString(problems);

        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 검증 실패: " + e.getMessage());
        }
    }

}
