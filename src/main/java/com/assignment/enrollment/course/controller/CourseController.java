package com.assignment.enrollment.course.controller;

import com.assignment.enrollment.course.dto.CourseCreateRequest;
import com.assignment.enrollment.course.dto.CourseResponse;
import com.assignment.enrollment.course.dto.CourseStatusUpdateRequest;
import com.assignment.enrollment.course.entity.CourseStatus;
import com.assignment.enrollment.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @PostMapping
    public Long createCourse(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return courseService.createCourse(userId, request);
    }

    @GetMapping
    public List<CourseResponse> getCourses(
            @RequestParam(required = false) CourseStatus status
    ) {
        return courseService.getCourses(status);
    }



    @PatchMapping("/{courseId}/status")
    public void updateCourseStatus(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseStatusUpdateRequest request
    ) {
        courseService.updateCourseStatus(userId, courseId, request);
    }
}
