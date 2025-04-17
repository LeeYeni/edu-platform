package com.example.education.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassReportDto {
    private int total;
    private int submitted;
    private List<String> notSubmittedIds;
    private List<WrongQuestionSummary> topWrongQuestions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WrongQuestionSummary {
        private int questionNum;
        private String questionText;
        private List<OptionDto> options;
        private List<AnswerStat> submissionStats;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptionDto {
        private String id;
        private String text;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnswerStat {
        private String answer;
        private double percentage;
    }
}

