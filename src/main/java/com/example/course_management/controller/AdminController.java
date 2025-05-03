package com.example.course_management.controller;

import com.example.course_management.dto.AbsenceRequestDTO;
import com.example.course_management.dto.CourseDTO;
import com.example.course_management.entity.AbsenceRequest;
import com.example.course_management.entity.Course;
import com.example.course_management.entity.User;
import com.example.course_management.service.AbsenceRequestService;
import com.example.course_management.service.CourseService;
import com.example.course_management.service.AuthService;
import com.example.course_management.service.EmailService;
import com.example.course_management.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AbsenceRequestService absenceRequestService;
    private final CourseService courseService;
    private final AuthService authService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public AdminController(AbsenceRequestService absenceRequestService,
                             CourseService courseService,
                             AuthService authService,
                             EmailService emailService,
                             UserRepository userRepository) {
        this.absenceRequestService = absenceRequestService;
        this.courseService = courseService;
        this.authService = authService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    // --- Absence Request Management ---
    @GetMapping("/absence-requests")
    public ResponseEntity<List<AbsenceRequestDTO>> getAllAbsenceRequests() {
        return ResponseEntity.ok(absenceRequestService.getAllRequestsAsDTO());
    }

    @PutMapping("/absence-requests/{id}/approve")
    public ResponseEntity<String> approveAbsenceRequest(@PathVariable Long id) {
        AbsenceRequest approvedRequest = absenceRequestService.updateRequestStatus(id, "APPROVED");
        if (approvedRequest == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Absence Request not found.");
        }

        try {
            // Notify teacher
            notifyTeacherAboutAbsenceApproval(approvedRequest);
            // Notify direction
            notifyDirectionAboutAbsenceApproval(approvedRequest);
            return ResponseEntity.ok("Absence Request approved and notifications sent");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Absence Request approved but failed to send notifications: " + e.getMessage());
        }
    }

    // --- Makeup Course Management (Teacher-Proposed) ---
    @PutMapping("/makeup-courses/{id}/approve")
    public ResponseEntity<String> approveMakeupCourse(@PathVariable Long id) {
        CourseDTO approvedCourseDTO = courseService.updateCourseStatus(id, "APPROVED"); // Get DTO
        if (approvedCourseDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Makeup Course not found or already processed.");
        }
        // Use the DTO to get the required data for the notification.
        notifyTeacherAboutMakeupApproval(approvedCourseDTO);
        notifyDirectionAboutMakeupApproval(approvedCourseDTO);
        return ResponseEntity.ok("Makeup Course approved and notifications sent");
    }

    // --- Makeup Course Management (Admin-Proposed) ---
    @PostMapping("/makeup-courses/propose")
    public ResponseEntity<?> proposeMakeupCourse(@RequestBody CourseDTO courseDTO) {
        Long teacherId = courseDTO.getTeacherId();
        if (teacherId == null) {
            return ResponseEntity.badRequest().body("Teacher ID must be provided in the CourseDTO.");
        }

        User teacher = userRepository.findById(teacherId)
                .orElse(null);

        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found with ID: " + teacherId);
        }
        if (!"ROLE_TEACHER".equals(teacher.getRole())) {
            return ResponseEntity.badRequest().body("Specified user is not a teacher.");
        }

        try {
            Course proposedCourse = new Course();
            proposedCourse.setCourseName(courseDTO.getCourseName());
            proposedCourse.setName(courseDTO.getCourseName());
            proposedCourse.setDescription(courseDTO.getDescription());
            proposedCourse.setTimetable(courseDTO.getTimetable());
            proposedCourse.setTeacher(teacher);
            proposedCourse.setType("MAKEUP");
            proposedCourse.setStatus("PENDING_TEACHER_APPROVAL");

            Course savedCourse = courseService.saveCourse(proposedCourse);
            //  Use the saved course to get all the data for notification.
            notifyTeacherAboutMakeupProposal(savedCourse);
            return ResponseEntity.ok(courseService.convertToDTO(savedCourse)); // Convert to DTO for response
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error proposing makeup course: " + e.getMessage());
        }
    }

    // --- Teacher Invitation ---
    @PostMapping("/invite-teacher")
    public ResponseEntity<String> inviteTeacher(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String fullName = payload.get("fullName");

        if (email == null || email.isBlank() || fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest().body("Email and full name are required.");
        }

        try {
            String token = UUID.randomUUID().toString();
            authService.createTeacherInvite(email, fullName, token);
            String inviteLink = "http://localhost:3000/complete-registration?token=" + token;
            emailService.sendInviteEmail(email, fullName, inviteLink);
            return ResponseEntity.ok("Invitation sent to " + email);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send invitation.");
        }
    }

    // --- Notification Helper Methods ---
    private void notifyTeacherAboutAbsenceApproval(AbsenceRequest request) {
        if (request.getTeacher() == null || request.getCourse() == null) return;
        String message = String.format(
                "Your absence request for course %s has been approved.",
                request.getCourse().getCourseName()
        );
        emailService.sendNotification(
                request.getTeacher().getEmail(),
                "Absence Request Approved",
                message
        );
    }

    private void notifyDirectionAboutAbsenceApproval(AbsenceRequest request) {
        if (request.getTeacher() == null || request.getCourse() == null) return;
        String message = String.format(
                "Teacher %s's absence for course %s has been approved.",
                request.getTeacher().getFullName(),
                request.getCourse().getCourseName()
        );
        emailService.sendNotificationToDirection(
                "New Approved Absence",
                message
        );
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    private void notifyTeacherAboutMakeupApproval(CourseDTO course) {
        //  moved the null check
        String teacherEmail = course.getTeacherEmail();
        if (teacherEmail == null || teacherEmail.isBlank()) {
            System.err.println("Cannot notify teacher about makeup approval - Email missing in DTO for course: " + course.getCourseName());
            return;
        }

        String message = String.format(
                "Your makeup course proposal '%s' has been approved by the admin.",
                course.getCourseName()
        );
        emailService.sendNotification(
                teacherEmail,
                "Makeup Course Approved",
                message
        );
    }

    private void notifyDirectionAboutMakeupApproval(CourseDTO course) {
        String message = String.format(
                "Makeup course '%s' proposed by %s has been approved by the admin.",
                course.getCourseName(),
                course.getTeacherName()
        );
        emailService.sendNotificationToDirection(
                "New Approved Makeup Course (Teacher-Proposed)",
                message
        );
    }

    private void notifyTeacherAboutMakeupProposal(Course course) {
        if (course.getTeacher() == null) return;
        String message = String.format(
                "Admin has proposed a makeup course '%s' for your approval. Please log in to review.",
                course.getCourseName()
        );
        emailService.sendNotification(
                course.getTeacher().getEmail(),
                "Makeup Course Proposal from Admin",
                message
        );
    }
}

