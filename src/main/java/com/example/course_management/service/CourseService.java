package com.example.course_management.service;

import com.example.course_management.dto.CourseDTO;
import com.example.course_management.entity.Course;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository, EmailService emailService, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public CourseDTO convertToDTO(Course course) {
        Long teacherId = null;
        String teacherName = "N/A";
        String teacherEmail = null;

        if (course.getTeacher() != null) {
            teacherId = course.getTeacher().getId();
            teacherName = course.getTeacher().getFullName();
            teacherEmail = course.getTeacher().getEmail();
        }

        return new CourseDTO(
                course.getId(),
                course.getCourseName(),
                teacherId,
                teacherName,
                teacherEmail,
                course.getStatus(),
                course.getType(),
                course.getDescription(),
                course.getTimetable() // Assuming Timetable is available in Course entity
        );
    }

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id).orElse(null);
        return (course != null) ? convertToDTO(course) : null;
    }

    @Transactional
    public Course saveCourse(Course course) {
        if (course.getTeacher() == null) {
            throw new RuntimeException("Teacher must be assigned to the course");
        }
        Course savedCourse = courseRepository.save(course);
        // Trigger notification if a new MAKEUP course is directly saved with APPROVED status
        if ("MAKEUP".equals(savedCourse.getType()) && "APPROVED".equals(savedCourse.getStatus())) {
             notifyStudentsAboutApprovedMakeup(savedCourse);
        }
        return savedCourse;
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    // Modified updateCourseStatus to potentially trigger notification if status changes to APPROVED
    @Transactional
    public CourseDTO updateCourseStatus(Long id, String status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + id));

        String oldStatus = course.getStatus(); // Get old status

        course.setStatus(status);
        Course updatedCourse = courseRepository.save(course);

        // Notify students if a MAKEUP course status changes to APPROVED
        if ("MAKEUP".equals(updatedCourse.getType()) && "APPROVED".equals(updatedCourse.getStatus()) && !"APPROVED".equals(oldStatus)) {
             notifyStudentsAboutApprovedMakeup(updatedCourse);
        }

        return convertToDTO(updatedCourse);
    }

    public List<CourseDTO> getApprovedCourses() {
        return courseRepository.findByStatus("APPROVED").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getCoursesByTeacher(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Propose a makeup course (for TEACHERS) - Admin approval required later
    @Transactional
    public Course proposeMakeupCourse(Course course) {
        course.setType("MAKEUP");
        course.setStatus("PENDING"); // Initially PENDING, requires Admin approval
        return courseRepository.save(course);
    }

    // --- Methods for Teacher handling Admin Proposals ---
    public List<CourseDTO> getPendingTeacherApprovalCourses(Long teacherId) {
        return courseRepository.findByTeacherIdAndStatus(teacherId, "PENDING_TEACHER_APPROVAL").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // This method is for teachers approving a makeup course *proposed by admin*
    // Assuming this ultimately leads to the 'APPROVED' status that Admin initiated.
    // If Admin's final approval happens *after* teacher approval, the notification
    // should be triggered by the method the Admin uses for final approval.
    // However, based on your earlier code, it seems teacher approval *is* the final step
    // for admin-proposed makeups before they become APPROVED.
    @Transactional
    public CourseDTO approveAdminProposedMakeup(Long courseId, Long teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Makeup course not found with ID: " + courseId));

        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Teacher ID mismatch or course not assigned to this teacher.");
        }
        if (!"PENDING_TEACHER_APPROVAL".equals(course.getStatus())) {
            throw new RuntimeException("Course is not awaiting teacher approval.");
        }
         if (!"MAKEUP".equals(course.getType())) {
             throw new RuntimeException("Course is not a makeup course.");
         }

        String oldStatus = course.getStatus(); // Get old status
        course.setStatus("APPROVED");
        Course savedCourse = courseRepository.save(course);

        // Notify students if status changed to APPROVED for a MAKEUP course
        if ("MAKEUP".equals(savedCourse.getType()) && "APPROVED".equals(savedCourse.getStatus()) && !"APPROVED".equals(oldStatus)) {
             notifyStudentsAboutApprovedMakeup(savedCourse);
        }

        notifyDirectionAboutTeacherApprovedMakeup(savedCourse);
        return convertToDTO(savedCourse);
    }

    @Transactional
    public CourseDTO rejectAdminProposedMakeup(Long courseId, Long teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Makeup course not found with ID: " + courseId));

        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Teacher ID mismatch or course not assigned to this teacher.");
        }
        if (!"PENDING_TEACHER_APPROVAL".equals(course.getStatus())) {
            throw new RuntimeException("Course is not awaiting teacher approval.");
        }
         if (!"MAKEUP".equals(course.getType())) {
             throw new RuntimeException("Course is not a makeup course.");
         }

        course.setStatus("REJECTED_BY_TEACHER");
        Course savedCourse = courseRepository.save(course);
        return convertToDTO(savedCourse);
    }

    private void notifyDirectionAboutTeacherApprovedMakeup(Course course) {
        String teacherName = course.getTeacher() != null ? course.getTeacher().getFullName() : "Unknown Teacher";
        String subject = "Admin-Proposed Makeup Course Approved by Teacher";
        String message = String.format(
                "Makeup course '%s' proposed by Admin has been approved by Teacher %s. Timetable: %s",
                course.getCourseName(),
                teacherName,
                course.getTimetable() // Assuming getTimetable() returns a suitable string
        );
        emailService.sendNotificationToDirection(subject, message);
    }

    // --- NEW METHOD: Notify students about approved makeup course ---
    private void notifyStudentsAboutApprovedMakeup(Course makeupCourse) {
        // Notify ALL users with role "STUDENT" as per the clarified requirement
        List<User> students = userRepository.findByRole("STUDENT"); // Get all students by role

        String subject = "Approved Makeup Course Notification";
        String teacherName = makeupCourse.getTeacher() != null ? makeupCourse.getTeacher().getFullName() : "Unknown Teacher";
        String timetableDetails = makeupCourse.getTimetable() != null ? makeupCourse.getTimetable() : "Details to follow"; // Assuming getTimetable() exists and works

        String message = String.format(
            "Dear Student,\n\nA makeup course has been approved:\n\n" +
            "Course Name: %s\n" +
            "Teacher: %s\n" +
            "Schedule: %s\n\n" + // Include timetable details
            "Please check the platform for more details.\n\n" +
            "Sincerely,\nThe Course Management Team",
            makeupCourse.getCourseName(),
            teacherName,
            timetableDetails
        );

        for (User student : students) {
            if (student.getEmail() != null && !student.getEmail().isEmpty()) {
                emailService.sendNotification(student.getEmail(), subject, message);
            } else {
                System.out.println("Student " + student.getFullName() + " has no email address to send notification.");
            }
        }
         System.out.println("Sent approved makeup course notification to " + students.size() + " students."); // Log for debugging
    }
    // --- END NEW METHOD ---

    // Fetch user by email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

}