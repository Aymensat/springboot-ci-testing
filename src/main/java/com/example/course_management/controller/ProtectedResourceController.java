package com.example.course_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RequestMapping("/api/protected")
public class ProtectedResourceController {

    @GetMapping("/resource")
    public ResponseEntity<String> getProtectedResource() {
        return ResponseEntity.ok("You have access to this protected resource!");
}


}