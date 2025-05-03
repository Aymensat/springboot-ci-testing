package com.example.course_management.controller;

import com.example.course_management.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final EmailService emailService;

    public TestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/send-test-email")
    public String sendTestEmail() {
        emailService.sendNotification(
            "youssef.gazdar@enicar.ucar.tn", // Replace with a valid email address for testing
            "Test Email",
            "This is a test email from the system."
        );
        return "Test email sent";
    }
}
