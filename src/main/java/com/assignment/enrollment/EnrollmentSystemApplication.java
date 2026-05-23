package com.assignment.enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class EnrollmentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentSystemApplication.class, args);
    }

}
