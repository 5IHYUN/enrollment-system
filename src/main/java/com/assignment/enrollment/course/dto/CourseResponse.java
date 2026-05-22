package com.assignment.enrollment.course.dto;

import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.entity.CourseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseResponse(
        Long courseId,
        String title,
        BigDecimal price,
        int capacity,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getPrice(),
                course.getCapacity(),
                course.getStartDate(),
                course.getEndDate(),
                course.getStatus()
        );
    }
}
