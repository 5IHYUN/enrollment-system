package com.assignment.enrollment.enrollment.dto;

import com.assignment.enrollment.enrollment.entity.Enrollment;
import com.assignment.enrollment.enrollment.entity.EnrollmentStatus;

import java.time.LocalDate;

public record EnrollmentResponse(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        EnrollmentStatus status,
        LocalDate startDate,
        LocalDate endDate
) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getStatus(),
                enrollment.getCourse().getStartDate(),
                enrollment.getCourse().getEndDate()
        );
    }
}