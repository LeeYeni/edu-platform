package com.example.education.controller;

import com.example.education.dto.LoginRequest;
import com.example.education.dto.SignupRequest;
import com.example.education.dto.UserDto;
import com.example.education.entity.User;
import com.example.education.service.UserService;
import com.example.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/api/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok("회원가입에 성공했습니다.");
    }

    // ✅ ID 중복 체크용 엔드포인트
    @GetMapping("/api/check-id")
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestParam String id) {
        boolean exists = userService.checkIdExists(id);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println(request);
        Optional<User> userOpt = userRepository.findById(request.getId());

        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of("name", user.getName(),
                "userType", user.getUserType(),
                "grade", user.getGrade(),
                "classname", user.getClassname(),
                "userId", user.getId()));
    }

    @GetMapping("/api/user/classroom/{roomCode}")
    public ResponseEntity<List<UserDto>> getClassStudents(@PathVariable String roomCode) {
        List<User> users = userRepository.findByIdStartingWith(roomCode + "-");
        System.out.println(users);
        List<UserDto> result = users.stream()
                .map(user -> new UserDto(user.getId(), user.getName(), user.getStudentId()))
                .sorted(Comparator.comparingInt(u -> Integer.parseInt(u.getStudentId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

}