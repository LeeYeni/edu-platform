package com.example.education.repository;

import com.example.education.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    // QuizResultRepository.java
    List<QuizResult> findByUserIdAndQuestionId(String userId, String questionId);
    List<QuizResult> findByUserIdOrderByQuestionIdAscQuestionNumAsc(String userId);

    Optional<QuizResult> findByUserIdAndQuestionIdAndQuestionNum(
            String userId, String questionId, int questionNum);

    boolean existsByUserIdAndQuestionId(String userId, String questionId);

    List<QuizResult> findByQuestionIdStartsWith(String prefix);

}
