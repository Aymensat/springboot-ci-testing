package com.example.course_management.controller;

import com.example.course_management.dto.AbsenceRequestDTO;
import com.example.course_management.dto.CourseDTO;
import com.example.course_management.service.AbsenceRequestService;
import com.example.course_management.service.CourseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/direction")
@CrossOrigin(origins = "http://localhost:3000")
public class DirectionController {
    private final CourseService courseService;
    private final AbsenceRequestService absenceRequestService;

    public DirectionController(CourseService courseService,
                                 AbsenceRequestService absenceRequestService) {
        this.courseService = courseService;
        this.absenceRequestService = absenceRequestService;
    }

    @GetMapping("/approved-courses")
    public List<CourseDTO> getApprovedCourses() {
        return courseService.getApprovedCourses();
    }

    @GetMapping("/approved-absences")
    public List<AbsenceRequestDTO> getApprovedAbsences() {
        return absenceRequestService.getApprovedAbsenceRequests();
    }
     /**
      * @param teacherId The ID of the teacher whose absence requests are to be fetched.
      * @return A list of absence request DTOs for the specified teacher.
      */
      @GetMapping("/teachers/{teacherId}/absence-requests")
      public ResponseEntity<List<AbsenceRequestDTO>> getAbsenceRequestsByTeacher(@PathVariable Long teacherId) {
          List<AbsenceRequestDTO> requests = absenceRequestService.getAbsenceRequestsByTeacher(teacherId); // Use the existing method
          return ResponseEntity.ok(requests);
      }
    /**
     * Gets all courses assigned to a specific teacher.
     * Accessible only by users with the DIRECTION role.
     *
     * @param teacherId The ID of the teacher whose courses are to be fetched.
     * @return A list of course DTOs for the specified teacher.
     */
    @GetMapping("/teachers/{teacherId}/courses")
    public ResponseEntity<List<CourseDTO>> getCoursesByTeacher(@PathVariable Long teacherId) {
        List<CourseDTO> courses = courseService.getCoursesByTeacher(teacherId); // Use the existing method
        return ResponseEntity.ok(courses);
    }
}