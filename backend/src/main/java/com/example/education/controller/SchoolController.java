package com.example.education.controller;

import com.example.education.dto.SignupRequest;
import com.example.education.entity.School;
import com.example.education.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolRepository schoolRepository;

    @GetMapping("/api/search-school")
    public ResponseEntity<Map<String, Object>> searchSchool(@RequestParam String name) {
        List<School> schools = schoolRepository.findByNameContainingIgnoreCase(name);

        for (School school : schools) {
            System.out.println("학교명: " + school.getName() + ", 학교코드: " + school.getCode());
        }
        // 필요한 정보만 담아서 전송
        List<Map<String, String>> result = schools.stream().map(s -> Map.of(
                "schoolName", s.getName(),          // ✅ getSchoolName() → getName()
                "schoolAddress", s.getAddress(),    // ✅ getSchoolAddress() → getAddress()
                "schoolCode", s.getCode()           // ✅ getSchoolCode() → getCode()
        )).toList();

        return ResponseEntity.ok(Map.of("schools", result));
    }
}