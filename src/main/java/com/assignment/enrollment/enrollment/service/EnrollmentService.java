package com.assignment.enrollment.enrollment.service;

import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.entity.CourseStatus;
import com.assignment.enrollment.course.repository.CourseRepository;
import com.assignment.enrollment.enrollment.dto.EnrollmentResponse;
import com.assignment.enrollment.enrollment.entity.Enrollment;
import com.assignment.enrollment.enrollment.entity.EnrollmentStatus;
import com.assignment.enrollment.enrollment.repository.EnrollmentRepository;
import com.assignment.enrollment.user.entity.User;
import com.assignment.enrollment.user.entity.UserRole;
import com.assignment.enrollment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    // 수강 신청
    @Transactional
    public Long enroll(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        // 수강생 검증
        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalStateException("수강생만 수강 신청할 수 있습니다.");
        }
        // 강의 row락 획득 (동시 접근 방지)
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        // 강의 상태 검증
        if (course.getStatus() != CourseStatus.OPEN) {
            throw new IllegalStateException("모집 중인 강의만 신청할 수 있습니다.");
        }
        // 이미 신청한 강의인지 검증 (CANCELLED 상태는 신청 가능)
        boolean alreadyEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        );

        if (alreadyEnrolled) {
            throw new IllegalStateException("이미 신청한 강의입니다.");
        }
        // 현재 신청 인원 계산
        long currentEnrollmentCount = enrollmentRepository.countByCourseIdAndStatusIn(
                courseId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );
        // 정원 초과 방지
        if (currentEnrollmentCount >= course.getCapacity()) {
            throw new IllegalStateException("수강 정원이 초과되었습니다.");
        }
        // 신청 엔티티 생성, db insert
        Enrollment enrollment = new Enrollment(user, course);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        return savedEnrollment.getId();
    }
    // 결제(상태 변경)
    @Transactional
    public void confirm(Long userId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강 신청입니다."));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 수강 신청만 확정할 수 있습니다.");
        }

        enrollment.confirm();
    }

    // 수강 취소
    @Transactional
    public void cancel(Long userId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강 신청입니다."));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 수강 신청만 취소할 수 있습니다.");
        }

        enrollment.cancel();
    }

    // 내 수강 신청 목록 조회
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(Long userId) {
        return enrollmentRepository.findByUserId(userId)
                .stream()
                .map(EnrollmentResponse::from)
                .toList();
    }
}
