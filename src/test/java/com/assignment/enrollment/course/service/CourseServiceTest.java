package com.assignment.enrollment.course.service;


import com.assignment.enrollment.course.dto.CourseCreateRequest;
import com.assignment.enrollment.course.dto.CourseDetailResponse;
import com.assignment.enrollment.course.dto.CourseResponse;
import com.assignment.enrollment.course.dto.CourseStatusUpdateRequest;
import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.entity.CourseStatus;
import com.assignment.enrollment.course.repository.CourseRepository;
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
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;


    @Test
    @DisplayName("크리에이터는 강의를 생성할 수 있다")
    void createCourse_success() {
        // given: 강의 생성 권한이 있는 크리에이터와 정상 요청 데이터가 주어진다
        Long userId = 1L;
        User creator = createUser(userId, UserRole.CREATOR);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course savedCourse = createCourse(10L, creator, request);

        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        // when: 강의 생성을 요청한다
        Long courseId = courseService.createCourse(userId, request);

        // then: 저장된 강의 ID를 반환하고 강의 저장 메서드가 호출된다
        assertThat(courseId).isEqualTo(10L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 강의를 생성할 수 없다")
    void createCourse_fail_userNotFound() {
        // given: 존재하지 않는 사용자 ID가 주어진다
        Long userId = 999L;

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then: 사용자 조회에 실패하면 예외가 발생하고 강의는 저장되지 않는다
        assertThatThrownBy(() -> courseService.createCourse(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");

        verify(courseRepository, never()).save(any(Course.class));
    }
    @Test
    @DisplayName("크리에이터가 아니면 강의를 생성할 수 없다")
    void createCourse_fail_notCreator() {
        // given: 강의 생성 권한이 없는 수강생 사용자가 주어진다
        Long userId = 2L;
        User student = createUser(userId, UserRole.STUDENT);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));

        // when & then: 크리에이터가 아니면 예외가 발생하고 강의는 저장되지 않는다
        assertThatThrownBy(() -> courseService.createCourse(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("크리에이터만 강의를 생성할 수 있습니다.");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("강의 시작일은 종료일보다 이전이어야 한다")
    void createCourse_fail_invalidPeriod() {
        // given: 시작일이 종료일보다 늦은 요청 데이터가 주어진다
        Long userId = 1L;
        User creator = createUser(userId, UserRole.CREATOR);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 6, 1)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));

        // when & then: 기간 조건을 만족하지 않으면 예외가 발생하고 강의는 저장되지 않는다
        assertThatThrownBy(() -> courseService.createCourse(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("강의 시작일은 종료일보다 이전이어야 합니다.");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("강의 시작일과 종료일이 같으면 강의를 생성할 수 없다")
    void createCourse_fail_sameStartDateAndEndDate() {
        // given: 시작일과 종료일이 같은 요청 데이터가 주어진다
        Long userId = 1L;
        User creator = createUser(userId, UserRole.CREATOR);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 1)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));

        // when & then: 시작일이 종료일보다 이전이 아니므로 예외가 발생한다.
        assertThatThrownBy(() -> courseService.createCourse(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("강의 시작일은 종료일보다 이전이어야 합니다.");

        verify(courseRepository, never()).save(any(Course.class));
    }


    @Test
    @DisplayName("강의 목록을 조회할 수 있다")
    void getCourses_success() {
        // given: OPEN 상태의 강의 목록이 주어진다
        User creator = createUser(1L, UserRole.CREATOR);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course1 = createCourse(1L, creator, request);
        Course course2 = createCourse(2L, creator, request);

        when(courseRepository.findByStatus(CourseStatus.OPEN))
                .thenReturn(List.of(course1, course2));

        // when: OPEN 상태 강의 목록을 조회한다
        List<CourseResponse> responses = courseService.getCourses(CourseStatus.OPEN);

        // then: 조회된 강의 목록이 반환된다
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
        assertThat(responses.get(1).courseId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("상태 조건이 없으면 OPEN 강의 목록을 조회한다")
    void getCourses_defaultOpenStatus() {
        // given: 상태 조건 없이 조회할 때 OPEN 강의가 반환되도록 설정한다
        User creator = createUser(1L, UserRole.CREATOR);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course = createCourse(1L, creator, request);

        when(courseRepository.findByStatus(CourseStatus.OPEN))
                .thenReturn(List.of(course));

        // when: 상태 조건 없이 강의 목록을 조회한다
        List<CourseResponse> responses = courseService.getCourses(null);

        // then: 기본값으로 OPEN 상태 강의가 조회된다
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(CourseStatus.DRAFT);
        verify(courseRepository).findByStatus(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상세를 조회할 수 있다")
    void getCourse_success() {
        // given: 존재하는 강의 ID가 주어진다
        Long courseId = 1L;
        User creator = createUser(1L, UserRole.CREATOR);

        CourseCreateRequest request = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course = createCourse(courseId, creator, request);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // when: 강의 상세를 조회한다
        CourseDetailResponse response = courseService.getCourse(courseId);

        // then: 강의 상세 정보가 반환된다
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.title()).isEqualTo("Spring Boot 입문");
        assertThat(response.description()).isEqualTo("스프링 부트 기초 강의입니다.");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.capacity()).isEqualTo(30);
        assertThat(response.currentEnrollmentCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("존재하지 않는 강의는 상세 조회할 수 없다")
    void getCourse_fail_courseNotFound() {
        // given: 존재하지 않는 강의 ID가 주어진다
        Long courseId = 999L;

        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // when & then: 강의 조회에 실패하면 예외가 발생한다
        assertThatThrownBy(() -> courseService.getCourse(courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 강의입니다.");
    }

    @Test
    @DisplayName("강의 생성자는 강의 상태를 DRAFT에서 OPEN으로 변경할 수 있다")
    void updateCourseStatus_success_draftToOpen() {
        // given: 강의 생성자와 DRAFT 상태의 강의가 주어진다
        Long userId = 1L;
        Long courseId = 10L;

        User creator = createUser(userId, UserRole.CREATOR);

        CourseCreateRequest createRequest = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course = createCourse(courseId, creator, createRequest);
        CourseStatusUpdateRequest request = new CourseStatusUpdateRequest(CourseStatus.OPEN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // when: 강의 상태 변경을 요청한다
        courseService.updateCourseStatus(userId, courseId, request);

        // then: 강의 상태가 OPEN으로 변경된다
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("본인이 생성한 강의가 아니면 상태를 변경할 수 없다")
    void updateCourseStatus_fail_notOwner() {
        // given: 강의 생성자가 아닌 다른 사용자가 주어진다
        Long creatorId = 1L;
        Long otherUserId = 2L;
        Long courseId = 10L;

        User creator = createUser(creatorId, UserRole.CREATOR);
        User otherUser = createUser(otherUserId, UserRole.CREATOR);

        CourseCreateRequest createRequest = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course = createCourse(courseId, creator, createRequest);
        CourseStatusUpdateRequest request = new CourseStatusUpdateRequest(CourseStatus.OPEN);

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // when & then: 본인이 생성한 강의가 아니면 예외가 발생한다
        assertThatThrownBy(() -> courseService.updateCourseStatus(otherUserId, courseId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("본인이 생성한 강의만 상태를 변경할 수 있습니다.");
    }

    @Test
    @DisplayName("잘못된 상태 전이는 실패한다")
    void updateCourseStatus_fail_invalidTransition() {
        // given: DRAFT 상태 강의를 CLOSED로 바로 변경하려고 한다
        Long userId = 1L;
        Long courseId = 10L;

        User creator = createUser(userId, UserRole.CREATOR);

        CourseCreateRequest createRequest = createCourseCreateRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        Course course = createCourse(courseId, creator, createRequest);
        CourseStatusUpdateRequest request = new CourseStatusUpdateRequest(CourseStatus.CLOSED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // when & then: DRAFT에서 CLOSED로 바로 변경할 수 없다
        assertThatThrownBy(() -> courseService.updateCourseStatus(userId, courseId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("변경할 수 없는 강의 상태입니다.");
    }

    private User createUser(Long id, UserRole role) {
        User user = new User("test", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private CourseCreateRequest createCourseCreateRequest(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new CourseCreateRequest(
                "Spring Boot 입문",
                "스프링 부트 기초 강의입니다.",
                BigDecimal.valueOf(50000),
                30,
                startDate,
                endDate
        );
    }

    private Course createCourse(
            Long id,
            User creator,
            CourseCreateRequest request
    ) {
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
        return course;
    }
}
