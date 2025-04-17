package com.example.education.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuizResultResponse {
    private Long id;
    private String userId;
    private String questionId;
    private Integer questionNum;
    private String userAnswer;
    private String correctAnswer;
    private boolean isCorrect;
}
