package com.example.course_management.service;

import com.example.course_management.dto.AbsenceRequestDTO;
import com.example.course_management.entity.AbsenceRequest;
import com.example.course_management.entity.Course; // Keep Course import
import com.example.course_management.repository.AbsenceRequestRepository;
import com.example.course_management.repository.CourseRepository; // Keep CourseRepository import
import com.example.course_management.repository.UserRepository; // Keep UserRepository import
import com.example.course_management.entity.User; // Keep User import

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AbsenceRequestService {

    private final UserRepository userRepository; // Keep UserRepository
    private final CourseRepository courseRepository; // Keep CourseRepository
    private final AbsenceRequestRepository absenceRequestRepository;
    private final EmailService emailService; // Inject EmailService

    public AbsenceRequestService(AbsenceRequestRepository absenceRequestRepository,
                                 UserRepository userRepository,
                                 CourseRepository courseRepository,
                                 EmailService emailService) { // Add EmailService to constructor
        this.absenceRequestRepository = absenceRequestRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.emailService = emailService; // Assign EmailService
    }

    // Conversion method to convert AbsenceRequest to AbsenceRequestDTO
    public AbsenceRequestDTO convertToDTO(AbsenceRequest request) {
        AbsenceRequestDTO dto = new AbsenceRequestDTO();
        dto.setId(request.getId()); // Set ID from entity
        dto.setJustification(request.getJustification());
        dto.setStatus(request.getStatus()); // Set Status from entity
        dto.setSubmittedAt(request.getSubmittedAt());

        if (request.getTeacher() != null) {
            dto.setTeacherId(request.getTeacher().getId());
            dto.setTeacherName(request.getTeacher().getFullName()); // Set teacher name
        }

        if (request.getCourse() != null) {
            dto.setCourseId(request.getCourse().getId());
            dto.setCourseName(request.getCourse().getCourseName()); // Assuming Course has getCourseName()
        }

        return dto;
    }

    // Convert List of AbsenceRequest to List of AbsenceRequestDTO
    public List<AbsenceRequestDTO> convertToDTOList(List<AbsenceRequest> requests) {
        return requests.stream()
                .<AbsenceRequestDTO>map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Service method to get all absence requests (might be for Admin)
    public List<AbsenceRequest> getAllRequests() {
        return absenceRequestRepository.findAll();
    }

    // Service method to submit an absence request (assuming this is for Teachers based on the DTO)
    @Transactional
    public AbsenceRequest submitRequest(AbsenceRequestDTO requestDTO) {
        User teacher = userRepository.findById(requestDTO.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        Course course = courseRepository.findById(requestDTO.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        AbsenceRequest request = new AbsenceRequest();
        request.setTeacher(teacher);
        request.setCourse(course);
        request.setJustification(requestDTO.getJustification());
        // Use submittedAt from DTO if provided, otherwise use current time
        request.setSubmittedAt(requestDTO.getSubmittedAt() != null ? requestDTO.getSubmittedAt() : LocalDateTime.now());
        request.setStatus("PENDING"); // Default status on submission

        return absenceRequestRepository.save(request);
    }

    // Service method to get absence requests by teacher ID
    public List<AbsenceRequestDTO> getAbsenceRequestsByTeacher(Long teacherId) {
        List<AbsenceRequest> requests = absenceRequestRepository.findByTeacherId(teacherId);
        return convertToDTOList(requests);
    }

    // Service method to get absence requests by teacher ID and Status
    public List<AbsenceRequestDTO> getAbsenceRequestsByTeacherAndStatus(Long teacherId, String status) {
        List<AbsenceRequest> requests = absenceRequestRepository.findByTeacherIdAndStatus(teacherId, status); // Use the new repository method
        return convertToDTOList(requests);
    }


    // Service method to get approved absence requests (might be for Admin or specific view)
    public List<AbsenceRequestDTO> getApprovedAbsenceRequests() {
        List<AbsenceRequest> approvedRequests = absenceRequestRepository.findByStatus("APPROVED");
        return convertToDTOList(approvedRequests);
    }

    // Service method to update absence request status (likely for Admin/Direction)
    @Transactional
    public AbsenceRequest updateRequestStatus(Long id, String status) {
        AbsenceRequest request = absenceRequestRepository.findById(id).orElse(null);
        if (request != null) {
            String oldStatus = request.getStatus(); // Get old status

            request.setStatus(status);
            AbsenceRequest updatedRequest = absenceRequestRepository.save(request);

            // --- NEW: Notify students if the status changes to APPROVED ---
            if ("APPROVED".equals(updatedRequest.getStatus()) && !"APPROVED".equals(oldStatus)) {
                notifyStudentsAboutApprovedAbsence(updatedRequest);
            }
            // --- END NEW ---

            return updatedRequest;
        }
        return null; // Or throw an exception if not found
    }

    public AbsenceRequest getById(Long id) {
        return absenceRequestRepository.findById(id).orElse(null);
    }

    public List<AbsenceRequestDTO> getAllRequestsAsDTO() {
        return convertToDTOList(absenceRequestRepository.findAll());
    }

    // You might need a method to get a specific absence request by ID as DTO
    public AbsenceRequestDTO getRequestByIdAsDTO(Long id) {
        AbsenceRequest request = absenceRequestRepository.findById(id).orElse(null);
        return (request != null) ? convertToDTO(request) : null;
    }

    // --- NEW METHOD: Notify students about an approved absence request ---
    private void notifyStudentsAboutApprovedAbsence(AbsenceRequest approvedRequest) {
        List<User> students = userRepository.findByRole("STUDENT"); // Get all students

        String subject = "Absence Request Approved";
        String teacherName = approvedRequest.getTeacher() != null ? approvedRequest.getTeacher().getFullName() : "Unknown Teacher";
        String courseName = approvedRequest.getCourse() != null ? approvedRequest.getCourse().getCourseName() : "Unknown Course";
        String justification = approvedRequest.getJustification() != null ? approvedRequest.getJustification() : "No justification provided.";

        String message = String.format(
            "Dear Student,\n\nAn absence request has been approved:\n\n" +
            "Teacher: %s\n" +
            "Course: %s\n" +
            "Justification: %s\n\n" +
            "Please check the course schedule for potential impacts or makeup information.\n\n" +
            "Sincerely,\nThe Course Management Team",
            teacherName,
            courseName,
            justification
        );

        for (User student : students) {
            if (student.getEmail() != null && !student.getEmail().isEmpty()) {
                emailService.sendNotification(student.getEmail(), subject, message);
            } else {
                System.out.println("Student " + student.getFullName() + " has no email address to send notification about approved absence.");
            }
        }
        System.out.println("Sent approved absence request notification to " + students.size() + " students."); // Log
    }
    // --- END NEW METHOD ---
}