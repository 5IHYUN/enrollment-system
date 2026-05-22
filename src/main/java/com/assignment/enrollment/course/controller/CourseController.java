package com.assignment.enrollment.course.controller;

import com.assignment.enrollment.course.dto.CourseCreateRequest;
import com.assignment.enrollment.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
