package com.example.education.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user")
@Getter
@Setter
@ToString
public class User {
    @Id
    @Column(name = "id")
    private String Id;;

    @Column(name = "name")
    private String name;
    @Column(name = "password")
    private String password;
    @Column(name = "user_type")
    private String userType;

    @Column(name = "school_name")
    private String schoolName;
    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "grade")
    private String grade;
    @Column(name = "class")
    private String classname;
    @Column(name = "student_id")
    private String studentId;
}
