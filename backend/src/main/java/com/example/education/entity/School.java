package com.example.education.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school")
@Getter
@Setter
public class School {

    @Id
    @Column(name = "school_code")
    private String code;

    @Column(name = "school_name")
    private String name;

    @Column(name = "school_address")
    private String address;
}
