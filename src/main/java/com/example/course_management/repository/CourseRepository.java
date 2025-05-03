package com.example.course_management.repository;

import com.example.course_management.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.example.course_management.entity.User; // Ensure User is imported

public interface CourseRepository extends JpaRepository<Course, Long> {
    // Custom query to find courses by teacher ID
    List<Course> findByTeacherId(Long teacherId);

    // Custom query to find courses by their status
    List<Course> findByStatus(String status);
    List<Course> findByType(String type);
    List<Course> findByTeacherIdAndType(Long teacherId, String type);
    List<Course> findByTeacherIdAndStatus(Long teacherId, String status);


}
