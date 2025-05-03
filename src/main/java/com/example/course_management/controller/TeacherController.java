package com.example.course_management.controller;

import com.example.course_management.dto.CourseDTO;
import com.example.course_management.entity.Course;
import com.example.course_management.entity.User;
import com.example.course_management.service.CourseService;
import com.example.course_management.service.AbsenceRequestService; // Import AbsenceRequestService
import com.example.course_management.dto.AbsenceRequestDTO; // Import AbsenceRequestDTO

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = "http://localhost:3000")
public class TeacherController {

    private final CourseService courseService;
    private final AbsenceRequestService absenceRequestService; // Inject AbsenceRequestService

    @Autowired
    public TeacherController(CourseService courseService, AbsenceRequestService absenceRequestService) { // Add to constructor
        this.courseService = courseService;
        this.absenceRequestService = absenceRequestService; // Assign
    }

    // Helper method remains the same
    private User getAuthenticatedTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            System.err.println("Unexpected principal type in SecurityContext: " + principal.getClass().getName());
            throw new AccessDeniedException("Could not retrieve user details from principal");
        }

        User teacher = courseService.getUserByEmail(username);

        if (teacher == null) {
             System.err.println("Authenticated principal's email found (" + username + "), but User entity not found in database.");
             throw new AccessDeniedException("Could not find user details for authenticated principal");
        }

         if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
              throw new AccessDeniedException("Authenticated user does not have the TEACHER role");
         }

        return teacher;
    }

    // Existing Course endpoints...

    @GetMapping("/courses")
    public List<CourseDTO> getCourses() {
        User teacher = getAuthenticatedTeacher();
        Long teacherId = teacher.getId();
        return courseService.getCoursesByTeacher(teacherId);
    }

    @PostMapping("/makeup-course")
    public ResponseEntity<CourseDTO> proposeMakeupCourse(@RequestBody CourseDTO courseDTO) {
        User teacher = getAuthenticatedTeacher();
        Course course = new Course();
        course.setCourseName(courseDTO.getCourseName());
        course.setName(courseDTO.getCourseName()); // Assuming 'name' is a property in Course
        course.setDescription(courseDTO.getDescription());
        course.setTimetable(courseDTO.getTimetable());
        course.setTeacher(teacher);
        Course savedCourse = courseService.proposeMakeupCourse(course);
        return ResponseEntity.ok(courseService.convertToDTO(savedCourse));
    }

    @GetMapping("/makeup-proposals/pending")
    public ResponseEntity<List<CourseDTO>> getPendingAdminMakeupProposals() {
        User teacher = getAuthenticatedTeacher();
        Long teacherId = teacher.getId();
        List<CourseDTO> pendingCourses = courseService.getPendingTeacherApprovalCourses(teacherId);
        return ResponseEntity.ok(pendingCourses);
    }

    @PutMapping("/makeup-proposals/{courseId}/approve")
    public ResponseEntity<CourseDTO> approveAdminMakeupProposal(@PathVariable Long courseId) {
        User teacher = getAuthenticatedTeacher();
        Long teacherId = teacher.getId();
        try {
            CourseDTO approvedCourse = courseService.approveAdminProposedMakeup(courseId, teacherId);
            return ResponseEntity.ok(approvedCourse);
        } catch (RuntimeException e) {
            System.err.println("Error approving makeup proposal: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/makeup-proposals/{courseId}/reject")
    public ResponseEntity<CourseDTO> rejectAdminMakeupProposal(@PathVariable Long courseId) {
        User teacher = getAuthenticatedTeacher();
        Long teacherId = teacher.getId();
        try {
            CourseDTO rejectedCourse = courseService.rejectAdminProposedMakeup(courseId, teacherId);
            return ResponseEntity.ok(rejectedCourse);
        } catch (RuntimeException e) {
             System.err.println("Error rejecting makeup proposal: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    // **New Absence Request Endpoints**

    @GetMapping("/absence-requests")
    public ResponseEntity<List<AbsenceRequestDTO>> getAllAbsenceRequestsForTeacher() {
        User teacher = getAuthenticatedTeacher(); // Get the authenticated teacher
        Long teacherId = teacher.getId();
        List<AbsenceRequestDTO> absenceRequests = absenceRequestService.getAbsenceRequestsByTeacher(teacherId);
        return ResponseEntity.ok(absenceRequests);
    }

    @GetMapping("/absence-requests/status/{status}")
    public ResponseEntity<List<AbsenceRequestDTO>> getAbsenceRequestsForTeacherByStatus(@PathVariable String status) {
        User teacher = getAuthenticatedTeacher(); // Get the authenticated teacher
        Long teacherId = teacher.getId();
        // You might want to validate the 'status' parameter here
        List<AbsenceRequestDTO> absenceRequests = absenceRequestService.getAbsenceRequestsByTeacherAndStatus(teacherId, status.toUpperCase()); // Convert status to uppercase to match entity enum/string
        return ResponseEntity.ok(absenceRequests);
    }
}