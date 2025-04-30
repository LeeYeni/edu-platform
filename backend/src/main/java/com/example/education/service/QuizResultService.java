package com.example.education.service;

import com.example.education.dto.QuizResultRequest;
import com.example.education.dto.StudentQuizReportDto;
import com.example.education.entity.Question;
import com.example.education.entity.QuizResult;
import com.example.education.entity.User;
import com.example.education.repository.QuestionRepository;
import com.example.education.repository.QuizResultRepository;
import com.example.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class QuizResultService {

    private final QuizResultRepository quizResultRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public void saveAll(QuizResultRequest req) {
        List<QuizResult> existing = quizResultRepository.findByUserIdAndQuestionId(req.getUserId(), req.getQuestionId());
        if (!existing.isEmpty()) {
            quizResultRepository.deleteAll(existing);
        }

        List<QuizResult> results = req.getResults().stream().map(entry -> QuizResult.builder()
                .userId(req.getUserId())
                .questionId(req.getQuestionId())
                .questionNum(entry.getQuestionNum())
                .userAnswer(entry.getUserAnswer())
                .correctAnswer(entry.getCorrectAnswer())
                .isCorrect(entry.isCorrect())
                .build()
        ).toList();

        quizResultRepository.saveAll(results);
    }

    // QuizResultService.java
    public List<QuizResult> getResultsByUserSorted(String userId) {
        return quizResultRepository.findByUserIdOrderByQuestionIdAscQuestionNumAsc(userId);
    }

    public void updateQuizResults(QuizResultRequest request) {
        for (QuizResultRequest.QuizResultEntry entry : request.getResults()) {
            Optional<QuizResult> existing = quizResultRepository.findByUserIdAndQuestionIdAndQuestionNum(
                    request.getUserId(), request.getQuestionId(), entry.getQuestionNum());

            if (existing.isPresent()) {
                QuizResult result = existing.get();
                result.setUserAnswer(entry.getUserAnswer());
                result.setCorrect(entry.isCorrect());
                quizResultRepository.save(result);  // update
            }
        }
    }

    public List<StudentQuizReportDto> getStudentQuizReportsByRoomCode(String roomCode) {
        String questionIdPrefix = "t-" + roomCode + "-";

        List<QuizResult> allResults = quizResultRepository.findByQuestionIdStartsWith(questionIdPrefix);

        Map<String, List<QuizResult>> groupedByStudent = allResults.stream()
                .collect(Collectors.groupingBy(QuizResult::getUserId));

        List<StudentQuizReportDto> resultList = new ArrayList<>();

        for (String studentId : groupedByStudent.keySet()) {
            Optional<User> student = userRepository.findById(studentId);
            if (student.isEmpty()) continue;

            List<QuizResult> studentResults = groupedByStudent.get(studentId);

            List<StudentQuizReportDto.ResultEntry> results = studentResults.stream().map(r -> {
                Optional<Question> questionOpt = questionRepository.findByQuestionIdAndQuestionNum(
                        r.getQuestionId(), r.getQuestionNum());

                String questionText = questionOpt.map(Question::getQuestionText).orElse("문제를 불러올 수 없습니다.");
                String questionType = questionOpt.map(Question::getQuestionType).orElse("unknown");

                List<String> options = new ArrayList<>();
                if (questionOpt.isPresent() && questionOpt.get().getOptions() != null) {
                    try {
                        String rawOptions = questionOpt.get().getOptions(); // JSON 형태의 문자열
                        List<Object> parsed = objectMapper.readValue(rawOptions, new TypeReference<List<Object>>() {});
                        options = parsed.stream().map(Object::toString).collect(Collectors.toList());
                    } catch (Exception e) {
                        System.out.println("⚠ 옵션 파싱 오류: " + e.getMessage());
                    }
                }

                return new StudentQuizReportDto.ResultEntry(
                        r.getQuestionNum(),
                        r.getUserAnswer(),
                        r.getCorrectAnswer(),
                        r.isCorrect(),
                        questionText,
                        questionType,
                        options,
                        r.getQuestionId()
                );
            }).collect(Collectors.toList());

            resultList.add(new StudentQuizReportDto(
                    studentId,
                    student.get().getName(),
                    student.get().getStudentId(),
                    results
            ));
        }

        return resultList;
    }



}
