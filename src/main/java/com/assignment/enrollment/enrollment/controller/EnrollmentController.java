package com.assignment.enrollment.enrollment.controller;


import com.assignment.enrollment.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PostMapping("/classes/{courseId}")
    public Long enroll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long courseId
    ) {
        return enrollmentService.enroll(userId, courseId);
    }
}
