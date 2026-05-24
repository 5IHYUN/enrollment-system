package com.assignment.enrollment.enrollment.service;
import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.entity.CourseStatus;
import com.assignment.enrollment.course.repository.CourseRepository;
import com.assignment.enrollment.course.dto.CourseCreateRequest;
import com.assignment.enrollment.enrollment.dto.EnrollmentResponse;
import com.assignment.enrollment.enrollment.entity.Enrollment;
import com.assignment.enrollment.enrollment.entity.EnrollmentStatus;
import com.assignment.enrollment.enrollment.repository.EnrollmentRepository;
import com.assignment.enrollment.user.entity.User;
import com.assignment.enrollment.user.entity.UserRole;
import com.assignment.enrollment.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("수강생은 OPEN 상태 강의에 수강 신청할 수 있다")
    void enroll_success() {
        // given: 수강생과 OPEN 상태의 강의가 주어진다
        Long userId = 2L;
        Long courseId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(courseId, creator, CourseStatus.OPEN, 30);

        Enrollment savedEnrollment = createEnrollment(10L, student, course, EnrollmentStatus.PENDING);

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        )).thenReturn(false);
        when(enrollmentRepository.countByCourseIdAndStatusIn(
                courseId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        )).thenReturn(0L);
        when(enrollmentRepository.findByUserIdAndCourseIdAndStatus(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        )).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

        // when: 수강 신청을 요청한다
        Long enrollmentId = enrollmentService.enroll(userId, courseId);

        // then: 신청 ID가 반환되고, 신청 정보가 저장된다
        assertThat(enrollmentId).isEqualTo(10L);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 수강 신청할 수 없다")
    void enroll_fail_userNotFound() {
        // given
        Long userId = 999L;
        Long courseId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(userId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");

        verify(courseRepository, never()).findByIdForUpdate(any());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강생이 아니면 수강 신청할 수 없다")
    void enroll_fail_notStudent() {
        // given
        Long userId = 1L;
        Long courseId = 1L;

        User creator = createUser(userId, UserRole.CREATOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(userId, courseId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수강생만 수강 신청할 수 있습니다.");

        verify(courseRepository, never()).findByIdForUpdate(any());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 강의에는 수강 신청할 수 없다")
    void enroll_fail_courseNotFound() {
        // given
        Long userId = 2L;
        Long courseId = 999L;

        User student = createUser(userId, UserRole.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(userId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 강의입니다.");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 강의에는 수강 신청할 수 없다")
    void enroll_fail_notOpenCourse() {
        // given
        Long userId = 2L;
        Long courseId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course draftCourse = createCourse(courseId, creator, CourseStatus.DRAFT, 30);

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(draftCourse));

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(userId, courseId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("모집 중인 강의만 신청할 수 있습니다.");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 PENDING 상태로 신청한 강의는 다시 신청할 수 없다")
    void enroll_fail_alreadyPending() {
        // given
        Long userId = 2L;
        Long courseId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(courseId, creator, CourseStatus.OPEN, 30);

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        )).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(userId, courseId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 신청한 강의입니다.");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("정원이 가득 찬 강의에는 수강 신청할 수 없다")
    void enroll_fail_capacityExceeded() {
        // given
        Long userId = 2L;
        Long courseId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(courseId, creator, CourseStatus.OPEN, 2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        )).thenReturn(false);
        when(enrollmentRepository.countByCourseIdAndStatusIn(
                courseId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        )).thenReturn(2L);

        // when & then
        assertThatThrownBy(() -> enrollmentService.enroll(userId, courseId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수강 정원이 초과되었습니다.");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소된 신청 내역이 있으면 새 row를 만들지 않고 PENDING으로 복구한다")
    void enroll_success_restoreCancelledEnrollment() {
        // given
        Long userId = 2L;
        Long courseId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(courseId, creator, CourseStatus.OPEN, 30);

        Enrollment cancelledEnrollment = createEnrollment(
                20L,
                student,
                course,
                EnrollmentStatus.CANCELLED
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        )).thenReturn(false);
        when(enrollmentRepository.countByCourseIdAndStatusIn(
                courseId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        )).thenReturn(0L);
        when(enrollmentRepository.findByUserIdAndCourseIdAndStatus(
                userId,
                courseId,
                EnrollmentStatus.CANCELLED
        )).thenReturn(Optional.of(cancelledEnrollment));

        // when
        Long enrollmentId = enrollmentService.enroll(userId, courseId);

        // then
        assertThat(enrollmentId).isEqualTo(20L);
        assertThat(cancelledEnrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("PENDING 상태의 신청은 결제 확정할 수 있다")
    void confirm_success() {
        // given
        Long userId = 2L;
        Long enrollmentId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(enrollmentId, student, course, EnrollmentStatus.PENDING);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when
        enrollmentService.confirm(userId, enrollmentId);

        // then
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("본인의 신청이 아니면 결제 확정할 수 없다")
    void confirm_fail_notOwner() {
        // given
        Long ownerId = 2L;
        Long requestUserId = 3L;
        Long enrollmentId = 1L;

        User owner = createUser(ownerId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(enrollmentId, owner, course, EnrollmentStatus.PENDING);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when & then
        assertThatThrownBy(() -> enrollmentService.confirm(requestUserId, enrollmentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("본인의 수강 신청만 확정할 수 있습니다.");
    }

    @Test
    @DisplayName("CONFIRMED 상태의 신청은 다시 결제 확정할 수 없다")
    void confirm_fail_alreadyConfirmed() {
        // given
        Long userId = 2L;
        Long enrollmentId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(enrollmentId, student, course, EnrollmentStatus.CONFIRMED);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when & then
        assertThatThrownBy(() -> enrollmentService.confirm(userId, enrollmentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("결제 대기 상태만 확정할 수 있습니다.");
    }

    @Test
    @DisplayName("PENDING 상태의 신청은 취소할 수 있다")
    void cancel_success_pending() {
        // given
        Long userId = 2L;
        Long enrollmentId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(enrollmentId, student, course, EnrollmentStatus.PENDING);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when
        enrollmentService.cancel(userId, enrollmentId);

        // then
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태의 신청도 취소할 수 있다")
    void cancel_success_confirmed() {
        // given
        Long userId = 2L;
        Long enrollmentId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(enrollmentId, student, course, EnrollmentStatus.CONFIRMED);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when
        enrollmentService.cancel(userId, enrollmentId);

        // then
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태라도 결제 확정 후 7일이 지나면 취소할 수 없다")
    void cancel_fail_confirmed_after_7_days() {
        // given
        Long userId = 2L;
        Long enrollmentId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        Course course = createCourse(1L, createUser(1L, UserRole.CREATOR), CourseStatus.OPEN, 30);
        Enrollment enrollment = createEnrollment(enrollmentId, student, course, EnrollmentStatus.CONFIRMED);

        ReflectionTestUtils.setField(
                enrollment,
                "confirmedAt",
                LocalDateTime.now().minusDays(8)
        );

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when & then
        assertThatThrownBy(() -> enrollmentService.cancel(userId, enrollmentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소 가능 기간이 지났습니다.");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getCancelledAt()).isNull();
    }


    @Test
    @DisplayName("이미 취소된 신청은 다시 취소할 수 없다")
    void cancel_fail_alreadyCancelled() {
        // given
        Long userId = 2L;
        Long enrollmentId = 1L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(enrollmentId, student, course, EnrollmentStatus.CANCELLED);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // when & then
        assertThatThrownBy(() -> enrollmentService.cancel(userId, enrollmentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 신청입니다.");
    }

    @Test
    @DisplayName("내 수강 신청 목록을 조회할 수 있다")
    void getMyEnrollments_success() {
        // given
        Long userId = 2L;

        User student = createUser(userId, UserRole.STUDENT);
        User creator = createUser(1L, UserRole.CREATOR);
        Course course = createCourse(1L, creator, CourseStatus.OPEN, 30);

        Enrollment enrollment = createEnrollment(1L, student, course, EnrollmentStatus.PENDING);

        when(enrollmentRepository.findByUserId(userId)).thenReturn(List.of(enrollment));

        // when
        List<EnrollmentResponse> responses = enrollmentService.getMyEnrollments(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).enrollmentId()).isEqualTo(1L);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
        assertThat(responses.get(0).courseTitle()).isEqualTo("Spring Boot 입문");
        assertThat(responses.get(0).status()).isEqualTo(EnrollmentStatus.PENDING);
    }

    private User createUser(Long id, UserRole role) {
        User user = new User("test", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Course createCourse(
            Long id,
            User creator,
            CourseStatus status,
            int capacity
    ) {
        CourseCreateRequest request = new CourseCreateRequest(
                "Spring Boot 입문",
                "스프링 부트 기초 강의입니다.",
                BigDecimal.valueOf(50000),
                capacity,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course = new Course(
                creator,
                request.title(),
                request.description(),
                request.price(),
                request.capacity(),
                request.startDate(),
                request.endDate()
        );

        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "status", status);

        return course;
    }

    private Enrollment createEnrollment(
            Long id,
            User user,
            Course course,
            EnrollmentStatus status
    ) {
        Enrollment enrollment = new Enrollment(user, course);
        ReflectionTestUtils.setField(enrollment, "id", id);

        if (status == EnrollmentStatus.CONFIRMED) {
            enrollment.confirm();
            return enrollment;
        }

        ReflectionTestUtils.setField(enrollment, "status", status);
        return enrollment;
    }
}