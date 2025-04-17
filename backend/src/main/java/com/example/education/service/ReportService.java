package com.example.education.service;

import com.example.education.dto.ClassReportDto;
import com.example.education.entity.Question;
import com.example.education.entity.QuizResult;
import com.example.education.entity.User;
import com.example.education.repository.QuestionRepository;
import com.example.education.repository.QuizResultRepository;
import com.example.education.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final UserRepository userRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public Map<String, ClassReportDto> generateReportsGroupedByQuestionId(String roomCode) {
        List<User> allStudents = userRepository.findByIdStartingWith(roomCode + "-");
        String questionIdPrefix = "t-" + roomCode + "-";

        List<QuizResult> allResults = quizResultRepository.findByQuestionIdStartsWith(questionIdPrefix);

        Map<String, List<QuizResult>> groupedByQuestionId = allResults.stream()
                .collect(Collectors.groupingBy(QuizResult::getQuestionId));

        Map<String, ClassReportDto> resultMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<QuizResult>> entry : groupedByQuestionId.entrySet()) {
            String questionId = entry.getKey();
            List<QuizResult> questionResults = entry.getValue();

            Set<String> submittedUserIds = questionResults.stream().map(QuizResult::getUserId).collect(Collectors.toSet());
            int submitted = submittedUserIds.size();

            List<String> notSubmittedIds = allStudents.stream()
                    .filter(u -> !submittedUserIds.contains(u.getId()))
                    .map(User::getStudentId)
                    .collect(Collectors.toList());

            Map<String, List<QuizResult>> groupedByQuestion = questionResults.stream()
                    .collect(Collectors.groupingBy(r -> r.getQuestionId() + "#" + r.getQuestionNum()));

            List<Map.Entry<String, List<QuizResult>>> topWrong = groupedByQuestion.entrySet().stream()
                    .sorted((a, b) -> {
                        long wrongA = a.getValue().stream().filter(r -> !r.isCorrect()).count();
                        long wrongB = b.getValue().stream().filter(r -> !r.isCorrect()).count();
                        return Long.compare(wrongB, wrongA);
                    })
                    .collect(Collectors.toList());

            List<ClassReportDto.WrongQuestionSummary> topWrongQuestions = new ArrayList<>();

            for (Map.Entry<String, List<QuizResult>> questionEntry : topWrong) {
                String[] split = questionEntry.getKey().split("#");
                String qid = split[0];
                int qnum = Integer.parseInt(split[1]);

                Optional<Question> q = questionRepository.findByQuestionIdAndQuestionNum(qid, qnum);
                if (q.isEmpty()) continue;

                Question question = q.get();

                List<ClassReportDto.OptionDto> optionList = new ArrayList<>();
                try {
                    String raw = question.getOptions();
                    if (raw != null && !raw.isEmpty()) {
                        List<Map<String, String>> parsed = objectMapper.readValue(raw, new TypeReference<>() {});
                        for (Map<String, String> opt : parsed) {
                            optionList.add(new ClassReportDto.OptionDto(opt.get("id"), opt.get("text")));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠ 옵션 파싱 오류: " + e.getMessage());
                }

                Map<String, Long> counts = questionEntry.getValue().stream()
                        .collect(Collectors.groupingBy(QuizResult::getUserAnswer, Collectors.counting()));

                long totalSubmissions = questionEntry.getValue().size();
                List<ClassReportDto.AnswerStat> stats = counts.entrySet().stream()
                        .map(e -> new ClassReportDto.AnswerStat(
                                e.getKey(),
                                Math.round((e.getValue() * 1000.0 / totalSubmissions)) / 10.0
                        ))
                        .collect(Collectors.toList());

                topWrongQuestions.add(new ClassReportDto.WrongQuestionSummary(
                        qnum,
                        question.getQuestionText(),
                        optionList,
                        stats
                ));
            }

            resultMap.put(questionId, new ClassReportDto(
                    allStudents.size(),
                    submitted,
                    notSubmittedIds,
                    topWrongQuestions
            ));
        }

        System.out.println(resultMap);
        return resultMap;
    }
}
