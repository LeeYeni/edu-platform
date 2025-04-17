package com.example.education.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 실제 PK

    private String questionId; // room code 역할
    private String userId;
    private int questionNum;
    private String questionType;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String options;

    private String answer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // 단원 정보 추가
    private String unit1;  // 대단원
    private String unit2;  // 중단원
    private String unit3;  // 소단원
}
