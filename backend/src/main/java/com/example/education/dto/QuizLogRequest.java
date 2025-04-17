package com.example.education.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QuizLogRequest {
    private String school;
    private String grade;
    private String subject;
    private String chapter;
    private String middle;
    private String small;
    private String numberOfProblems;
    private String userType;
    private String userId;
}