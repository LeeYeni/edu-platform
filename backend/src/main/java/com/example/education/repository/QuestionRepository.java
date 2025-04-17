package com.example.education.repository;

import com.example.education.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
  // 사용자 문제 개수
    // ✅ 사용자별 questionId(문제 묶음) 개수 세기 (distinct count)
    @Query("SELECT COUNT(DISTINCT q.questionId) FROM Question q WHERE q.userId = :userId")
    long countDistinctQuestionIdByUserId(@Param("userId") String userId);

    List<Question> findByQuestionId(String roomCode);

    // QuestionRepository.java
    @Query("SELECT DISTINCT q FROM Question q WHERE q.userId = :userId")
    List<Question> findDistinctQuestionsByUserId(@Param("userId") String userId);

    List<Question> findByQuestionIdStartingWith(String teacherIdPrefix);

  Optional<Question> findByQuestionIdAndQuestionNum(String questionId, int questionNum);
}
