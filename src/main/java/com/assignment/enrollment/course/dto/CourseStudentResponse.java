package com.assignment.enrollment.course.dto;

import com.assignment.enrollment.enrollment.entity.Enrollment;
import com.assignment.enrollment.enrollment.entity.EnrollmentStatus;

public record CourseStudentResponse(
        Long userId,
        String name,
        EnrollmentStatus status
) {
    public static CourseStudentResponse from(Enrollment enrollment) {
        return new CourseStudentResponse(
                enrollment.getUser().getId(),
                enrollment.getUser().getName(),
                enrollment.getStatus()
        );
    }
}