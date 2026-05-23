package com.assignment.enrollment.course.controller;

import com.assignment.enrollment.course.dto.CourseCreateRequest;
import com.assignment.enrollment.course.dto.CourseDetailResponse;
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

    // 강의 생성
    @PostMapping
    public Long createCourse(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return courseService.createCourse(userId, request);
    }
    // 강의 목록 조회
    @GetMapping
    public List<CourseResponse> getCourses(
            @RequestParam(required = false) CourseStatus status
    ) {
        return courseService.getCourses(status);
    }
    // 강의 상세 조회
    @GetMapping("/{courseId}")
    public CourseDetailResponse getCourse(
            @PathVariable Long courseId
    ) {
        return courseService.getCourse(courseId);
    }

    // 강의 상태 변경
    @PatchMapping("/{courseId}/status")
    public void updateCourseStatus(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseStatusUpdateRequest request
    ) {
        courseService.updateCourseStatus(userId, courseId, request);
    }
}
