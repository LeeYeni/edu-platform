package com.example.education.dto;

import com.example.education.entity.Question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionWithAnswerDto {
    private String questionId;
    private int questionNum;
    private String questionText;
    private String options;
    private String answer;
    private String unit1;
    private String unit2;
    private String unit3;

    public QuestionWithAnswerDto(Question q) {
        this.questionId = q.getQuestionId();
        this.questionNum = q.getQuestionNum();
        this.questionText = q.getQuestionText();
        this.options = q.getOptions();
        this.answer = q.getAnswer(); // ✅ 여기가 핵심
        this.unit1 = q.getUnit1();
        this.unit2 = q.getUnit2();
        this.unit3 = q.getUnit3();
    }
}
