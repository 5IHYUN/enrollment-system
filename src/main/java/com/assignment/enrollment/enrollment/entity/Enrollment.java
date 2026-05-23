package com.assignment.enrollment.enrollment.entity;

import com.assignment.enrollment.common.entity.BaseEntity;
import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollments_user_class",
                        columnNames = {"user_id", "class_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public Enrollment(User user, Course course) {
        this.user = user;
        this.course = course;
        this.status = EnrollmentStatus.PENDING;
    }
    public void confirm() {
        if (this.status != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태만 확정할 수 있습니다.");
        }
        this.confirmedAt = LocalDateTime.now();
        this.status = EnrollmentStatus.CONFIRMED;
    }
    public void cancel() {
        if (this.status == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 신청입니다.");
        }
//        if (this.status == EnrollmentStatus.CONFIRMED
//                && this.confirmedAt.plusDays(7).isBefore(LocalDateTime.now())) {
//            throw new IllegalStateException("취소 가능 기간이 지났습니다.");
//        }
        this.cancelledAt = LocalDateTime.now();
        this.status = EnrollmentStatus.CANCELLED;
    }
}