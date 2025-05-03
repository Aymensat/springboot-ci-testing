package com.example.course_management.repository;

import com.example.course_management.entity.AbsenceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
    List<AbsenceRequest> findByTeacherId(Long teacherId);
    List<AbsenceRequest> findByStatus(String status); // This needs to be combined with teacherId
    // Add a method to find by teacher ID AND status
    List<AbsenceRequest> findByTeacherIdAndStatus(Long teacherId, String status); // **Add this method**
}