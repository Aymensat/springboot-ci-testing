package com.example.course_management.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${direction.email}")
    private String directionEmail;

    private final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendInviteEmail(String toEmail, String fullName, String link) {
        String subject = "You're invited to Course Management Platform";
        String text = "Hi " + fullName + ",\n\nYou've been invited to join as a teacher.\nClick the link to set your password: " + link;
        sendEmail(toEmail, subject, text);
    }

    public void sendNotification(String toEmail, String subject, String message) {
        sendEmail(toEmail, subject, message);
    }

    public void sendNotificationToDirection(String subject, String message) {
        sendEmail(directionEmail, subject, message);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Email sent successfully to {}", to);
        } catch (MailException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            // Optionally rethrow or handle differently depending on business logic
        }
    }
}
