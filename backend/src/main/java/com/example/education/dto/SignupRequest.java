package com.example.education.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SignupRequest {
    @JsonProperty("Id")
    private String Id;

    private String name;
    private String password;
    private String userType;

    private String schoolName;
    private String schoolCode;

    private String grade;
    private String classname;

    private String studentId;
}