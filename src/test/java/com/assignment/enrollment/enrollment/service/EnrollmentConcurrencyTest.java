package com.assignment.enrollment.enrollment.service;

import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.repository.CourseRepository;
import com.assignment.enrollment.enrollment.entity.EnrollmentStatus;
import com.assignment.enrollment.enrollment.repository.EnrollmentRepository;
import com.assignment.enrollment.user.entity.User;
import com.assignment.enrollment.user.entity.UserRole;
import com.assignment.enrollment.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
public class EnrollmentConcurrencyTest {
    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void setUp() {
        // FK 관계 때문에 enrollments → classes → users 순서로 삭제
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("정원이 1명인 강의에 동시에 여러 명이 신청해도 1명만 신청된다")
    void enroll_concurrency_capacityOne() throws Exception {
        // given
        int threadCount = 10;
        int capacity = 1;

        User creator = userRepository.save(new User("creator", UserRole.CREATOR));

        Course course = new Course(
                creator,
                "동시성 테스트 강의",
                "정원 1명 강의에 여러 사용자가 동시에 신청하는 테스트입니다.",
                BigDecimal.valueOf(50000),
                capacity,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        // 수강 신청 가능하도록 DRAFT → OPEN 변경
        course.updateStatus(com.assignment.enrollment.course.entity.CourseStatus.OPEN);

        Course savedCourse = courseRepository.save(course);
        Long courseId = savedCourse.getId();

        List<User> students = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            students.add(userRepository.save(new User("student" + i, UserRole.STUDENT)));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 모든 쓰레드가 준비될 때까지 기다리기 위한 latch
        CountDownLatch readyLatch = new CountDownLatch(threadCount);

        // 모든 쓰레드를 동시에 출발시키기 위한 latch
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            Long userId = students.get(i).getId();

            futures.add(executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    enrollmentService.enroll(userId, courseId);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        // 모든 쓰레드가 준비될 때까지 대기
        readyLatch.await();

        // 동시에 시작
        startLatch.countDown();

        // 모든 작업 종료 대기
        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        // then
        long actualEnrollmentCount = enrollmentRepository.countByCourseIdAndStatusIn(
                courseId,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );

        assertThat(actualEnrollmentCount).isEqualTo(capacity);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);
    }
}
