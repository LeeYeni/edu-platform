package com.example.education.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuizResultRequest {

    private String userId;
    private String questionId; // 문제 묶음 식별자 (roomCode)
    private List<QuizResultEntry> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class QuizResultEntry {
        private int questionNum;
        private String userAnswer;
        private String correctAnswer;
        @JsonProperty("isCorrect")
        private boolean isCorrect;
    }
}
