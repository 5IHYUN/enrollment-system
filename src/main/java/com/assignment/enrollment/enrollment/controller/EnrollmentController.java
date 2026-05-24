package com.assignment.enrollment.enrollment.controller;


import com.assignment.enrollment.enrollment.dto.EnrollmentResponse;
import com.assignment.enrollment.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    // 수강 신청
    @PostMapping("/classes/{courseId}")
    public Long enroll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long courseId
    ) {
        return enrollmentService.enroll(userId, courseId);
    }

    // 결제 확정
    @PatchMapping("/{enrollmentId}/confirm")
    public void confirm(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.confirm(userId, enrollmentId);
    }
    // 수강 취소
    @PatchMapping("/{enrollmentId}/cancel")
    public void cancel(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.cancel(userId, enrollmentId);
    }
    // 내 수강 신청 목록 조회
    @GetMapping("/me")
    public List<EnrollmentResponse> getMyEnrollments(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentService.getMyEnrollments(userId);
    }

}
