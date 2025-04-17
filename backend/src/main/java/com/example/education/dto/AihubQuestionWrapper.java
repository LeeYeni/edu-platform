package com.example.education.dto;

import lombok.Data;
import java.util.List;

@Data
public class AihubQuestionWrapper {
    private String question_filename;
    private String id;
    private List<QuestionInfo> question_info;

    @Data
    public static class QuestionInfo {
        private String question_grade;
        private int question_term;
        private String question_unit;
        private String question_topic;
        private String question_topic_name;
        private String question_type1;
        private int question_type2;
        private int question_condition;
        private String question_sector1;
        private String question_sector2;
        private String question_step;
        private int question_difficulty;
        private int question_contents;
    }
}