package com.example.education.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_result")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String questionId;   // 문제 묶음 ID
    private int questionNum;

    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;

    private boolean isCorrect;
}
