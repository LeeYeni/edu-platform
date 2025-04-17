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
}
