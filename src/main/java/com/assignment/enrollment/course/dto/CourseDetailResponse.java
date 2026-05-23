package com.assignment.enrollment.course.dto;

import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.entity.CourseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseDetailResponse(
        Long courseId,
        String title,
        String description,
        BigDecimal price,
        int capacity,
        long currentEnrollmentCount,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status
) {

    public static CourseDetailResponse of(
            Course course,
            long currentEnrollmentCount
    ) {
        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.getCapacity(),
                currentEnrollmentCount,
                course.getStartDate(),
                course.getEndDate(),
                course.getStatus()
        );
    }
}