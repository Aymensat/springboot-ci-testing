package com.example.course_management.controller;

import com.example.course_management.entity.User;
import com.example.course_management.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
    Optional<User> userOpt = userRepository.findByUsernameOrEmail(request.username(), request.username());

    if (userOpt.isEmpty()) {
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    User user = userOpt.get();

    if (!user.getPassword().equals(request.password())) {
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    if (!user.isActivated()) {
        return ResponseEntity.status(403).body("Account not activated");
    }

    // Create session and store user details
    HttpSession session = httpServletRequest.getSession();
    session.setAttribute("SPRING_SECURITY_CONTEXT", 
        new SecurityContextImpl(
            new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
            )
        )
    );

    return ResponseEntity.ok(Map.of(
        "message", "Login successful",
        "user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "role", user.getRole()
        )
    ));
}
        
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()) ||
                userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body("Username or email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password()); // Storing plain text password
        user.setRole(request.role());
        user.setActivated(true);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )
        ));
    }
}

// Simple DTO classes
record LoginRequest(String username, String password) {}
record RegisterRequest(String username, String email, String password, String role) {}