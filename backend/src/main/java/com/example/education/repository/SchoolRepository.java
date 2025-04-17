package com.example.education.repository;

import com.example.education.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, String> {
    List<School> findByNameContainingIgnoreCase(String name);
}
