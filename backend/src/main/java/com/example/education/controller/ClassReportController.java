package com.example.education.controller;

import com.example.education.dto.ClassReportDto;
import com.example.education.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ClassReportController {

    private final ReportService reportService;

    @GetMapping("/classroom/{roomCode}")
    public ResponseEntity<Map<String, Object>> getGroupedClassReport(@PathVariable String roomCode) {
        Map<String, ClassReportDto> reportMap = reportService.generateReportsGroupedByQuestionId(roomCode);

        Map<String, Object> response = new HashMap<>();
        response.put("reportByQuestionId", reportMap);  // 프론트에서 접근할 key
        return ResponseEntity.ok(response);
    }


}
