package com.example.education.service;

import com.example.education.dto.SignupRequest;
import com.example.education.repository.UserRepository;
import com.example.education.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void registerUser(SignupRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setPassword(request.getPassword());
        user.setUserType(request.getUserType());
        user.setSchoolName(request.getSchoolName());
        user.setSchoolCode(request.getSchoolCode());
        user.setGrade(request.getGrade());
        user.setClassname(request.getClassname());
        user.setStudentId(request.getStudentId());
        user.setId(request.getId());  // 사용자가 직접 생성한 아이디

        userRepository.save(user);
    }

    public boolean checkIdExists(String id) {
        return userRepository.existsById(id);
    }
}
