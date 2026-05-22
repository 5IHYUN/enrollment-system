package com.assignment.enrollment.course.service;


import com.assignment.enrollment.course.dto.CourseCreateRequest;
import com.assignment.enrollment.course.dto.CourseResponse;
import com.assignment.enrollment.course.entity.Course;
import com.assignment.enrollment.course.entity.CourseStatus;
import com.assignment.enrollment.course.repository.CourseRepository;
import com.assignment.enrollment.user.entity.User;
import com.assignment.enrollment.user.entity.UserRole;
import com.assignment.enrollment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;

    private final UserRepository userRepository;
    // 강의 생성
    @Transactional
    public Long createCourse(Long userId, CourseCreateRequest request) {
        // 유저 존재 확인
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        // 크리에이터만 생성 가능
        if (creator.getRole() != UserRole.CREATOR) {
            throw new IllegalStateException("크리에이터만 강의를 생성할 수 있습니다.");
        }
        // 강의 시작일 조건
        if (!request.startDate().isBefore(request.endDate())) {
            throw new IllegalArgumentException("강의 시작일은 종료일보다 이전이어야 합니다.");
        }

        Course course = new Course(
                creator,
                request.title(),
                request.description(),
                request.price(),
                request.capacity(),
                request.startDate(),
                request.endDate()
        );

        Course savedCourse = courseRepository.save(course);
        return savedCourse.getId();
    }
    // 강의 조회
    public List<CourseResponse> getCourses(CourseStatus status) {
        CourseStatus searchStatus = status == null ? CourseStatus.OPEN : status;

        return courseRepository.findByStatus(searchStatus)
                .stream()
                .map(CourseResponse::from)
                .toList();
    }
    // 강의 상세 조회

    // 강의 상태 변경



}
