package com.example.course_management.dto;

import lombok.AllArgsConstructor; // Add AllArgsConstructor
import lombok.Data;
import lombok.NoArgsConstructor; // Add NoArgsConstructor
import java.time.LocalDateTime;

@Data
@NoArgsConstructor // Add NoArgsConstructor
@AllArgsConstructor // Add AllArgsConstructor
public class AbsenceRequestDTO {
    private Long id; // Added ID
    private Long teacherId;
    private String teacherName; // Added teacher name
    private Long courseId;
    private String courseName; // Added course name
    private String justification;
    private String status; // Added Status
    private LocalDateTime submittedAt;
}