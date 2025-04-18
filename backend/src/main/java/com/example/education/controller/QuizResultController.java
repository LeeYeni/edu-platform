package com.example.education.controller;

import com.example.education.dto.QuestionWithAnswerDto;
import com.example.education.dto.QuizResultRequest;
import com.example.education.dto.QuizResultResponse;
import com.example.education.dto.StudentQuizReportDto;
import com.example.education.entity.Question;
import com.example.education.entity.QuizResult;
import com.example.education.repository.QuestionRepository;
import com.example.education.repository.QuizResultRepository;
import com.example.education.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizResultController {

    private final QuizResultService quizResultService;
    private final QuestionRepository questionRepository;
    private final QuizResultRepository quizResultRepository;

    @GetMapping("/result/exists")
    public boolean doesResultExist(@RequestParam String userId, @RequestParam String questionId) {
        return quizResultRepository.existsByUserIdAndQuestionId(userId, questionId);
    }

    @PostMapping("/saveResult")
    public ResponseEntity<String> saveQuizResult(@RequestBody QuizResultRequest req) {
        System.out.println(req);
        quizResultService.saveAll(req);
        return ResponseEntity.ok("저장 완료");
    }

    @GetMapping("/results/{userId}")
    public List<QuizResultResponse> getResultsByUser(@PathVariable String userId) {
        List<QuizResult> results = quizResultService.getResultsByUserSorted(userId);

        return results.stream()
                .map(r -> new QuizResultResponse(
                        r.getId(),
                        r.getUserId(),
                        r.getQuestionId(),
                        r.getQuestionNum(),
                        r.getUserAnswer(),
                        r.getCorrectAnswer(),
                        r.isCorrect() // ✅ 정확한 메서드명 (두 번째 'i'만 대문자)
                ))
                .collect(Collectors.toList());

    }


    @GetMapping("/created/{userId}")
    public ResponseEntity<List<Question>> getCreatedQuizzes(@PathVariable String userId) {
        List<Question> createdQuestions = questionRepository.findDistinctQuestionsByUserId(userId);
        return ResponseEntity.ok(createdQuestions);
    }

    @PutMapping("/updateResult")
    public ResponseEntity<String> updateQuizResult(@RequestBody QuizResultRequest request) {
        quizResultService.updateQuizResults(request);
        return ResponseEntity.ok("결과 업데이트 완료");
    }

    @GetMapping("/classroom/{schoolCode}/{grade}/{className}")
    public List<QuestionWithAnswerDto> getClassroomQuizzesWithAnswer(@PathVariable String schoolCode,
                                                                     @PathVariable String grade,
                                                                     @PathVariable String className) {
        String teacherIdPrefix = "t-" + schoolCode + "-" + grade + "-" + className;
        return questionRepository.findByQuestionIdStartingWith(teacherIdPrefix)
                .stream()
                .map(QuestionWithAnswerDto::new)
                .collect(Collectors.toList());
    }


    @GetMapping("/students/{roomCode}")
    public ResponseEntity<List<StudentQuizReportDto>> getStudentReports(@PathVariable String roomCode) {
        List<StudentQuizReportDto> reports = quizResultService.getStudentQuizReportsByRoomCode(roomCode);
        System.out.println(ResponseEntity.ok(reports));
        return ResponseEntity.ok(reports);
    }

}
