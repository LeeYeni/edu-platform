package com.example.education.repository;

import com.example.education.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    // String: User 엔티티의 PK 타입 (Id가 String 타입이었어요)

    // 필요 시 사용자 정의 메서드도 추가 가능
    boolean existsById(String Id); // 중복 확인용 (선택 사항)

    List<User> findByIdStartingWith(String s);
}