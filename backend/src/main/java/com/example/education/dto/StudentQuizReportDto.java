package com.example.education.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentQuizReportDto {
    private String userId;
    private String name;
    private String studentId;
    private List<ResultEntry> results;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResultEntry {
        private int questionNum;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String questionText;

        private String questionType;        // ✅ 문제 유형 (e.g. 객관식, OX 등)
        private List<String> options;       // ✅ 선택지 목록 (객관식인 경우)
        private String questionId;
    }
}
