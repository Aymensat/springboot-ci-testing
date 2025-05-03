package com.example.course_management.controller;

import com.example.course_management.dto.CourseDTO;
import com.example.course_management.entity.Course;
import com.example.course_management.service.CourseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/courses")
@CrossOrigin(origins = "http://localhost:3000")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseDTO> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public CourseDTO getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @PostMapping
    public CourseDTO saveCourse(@RequestBody CourseDTO courseDTO) { // Use CourseDTO
        Course course = new Course();
        course.setCourseName(courseDTO.getCourseName());
        course.setName(courseDTO.getCourseName()); // Set the 'name' property as well
        course.setDescription(courseDTO.getDescription());
        course.setTimetable(courseDTO.getTimetable());
        //  course.setTeacher(courseDTO.getTeacher()); //removed this, the service should handle it
        Course savedCourse = courseService.saveCourse(course);
        return courseService.convertToDTO(savedCourse); // Use the service method to convert
    }

    @PutMapping("/{id}/status")
    public CourseDTO updateCourseStatus(@PathVariable Long id, @RequestParam String status) {
        return courseService.updateCourseStatus(id, status);
    }

    @GetMapping("/approved")
    public List<CourseDTO> getApprovedCourses() {
        return courseService.getApprovedCourses();
    }

    @GetMapping("/teacher/{teacherId}")
    public List<CourseDTO> getCoursesByTeacher(@PathVariable Long teacherId) {
        return courseService.getCoursesByTeacher(teacherId);
    }
}