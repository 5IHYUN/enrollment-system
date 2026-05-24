package com.assignment.enrollment.enrollment.repository;

import com.assignment.enrollment.enrollment.entity.Enrollment;
import com.assignment.enrollment.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseIdAndStatusNot(
            Long userId,
            Long courseId,
            EnrollmentStatus status
    );

    long countByCourseIdAndStatusIn(
            Long courseId,
            List<EnrollmentStatus> statuses
    );

    List<Enrollment> findByUserId(Long userId);


    Optional<Enrollment> findByUserIdAndCourseIdAndStatus(
            Long userId,
            Long courseId,
            EnrollmentStatus status
    );

    List<Enrollment> findAllByCourseIdAndStatusIn(
            Long courseId,
            List<EnrollmentStatus> statuses
    );

}