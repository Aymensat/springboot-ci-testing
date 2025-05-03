package com.example.course_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDTO {
    private Long id;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    private String status;
    private String type;
    private String description;
    private String timetable;
}