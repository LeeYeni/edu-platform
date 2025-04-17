package com.example.education.controller;

import com.example.education.entity.Question;
import com.example.education.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuestionRepository questionRepository;

    @GetMapping("/play/{roomCode}")
    public ResponseEntity<List<Question>> getQuestionsByRoomCode(@PathVariable("roomCode") String roomCode) {
        List<Question> questions = questionRepository.findByQuestionId(roomCode);
        return ResponseEntity.ok(questions);
    }
}
