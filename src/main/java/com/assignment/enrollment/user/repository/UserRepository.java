package com.assignment.enrollment.user.repository;

import com.assignment.enrollment.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
