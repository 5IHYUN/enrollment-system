SET NAMES utf8mb4;

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL
);

CREATE TABLE classes (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         creator_id BIGINT NOT NULL,
                         title VARCHAR(100) NOT NULL,
                         description VARCHAR(255),
                         price DECIMAL(10, 2) NOT NULL,
                         capacity INT NOT NULL,
                         start_date DATE NOT NULL,
                         end_date DATE NOT NULL,
                         status VARCHAR(20) NOT NULL,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,

                         CONSTRAINT fk_classes_creator
                             FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE enrollments (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             class_id BIGINT NOT NULL,
                             status VARCHAR(20) NOT NULL,
                             confirmed_at DATETIME(6),
                             cancelled_at DATETIME(6),
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,

                             CONSTRAINT uk_enrollments_user_class UNIQUE (user_id, class_id),

                             CONSTRAINT fk_enrollments_user
                                 FOREIGN KEY (user_id) REFERENCES users(id),

                             CONSTRAINT fk_enrollments_class
                                 FOREIGN KEY (class_id) REFERENCES classes(id)
);

-- 테스트용 기본 사용자 데이터
INSERT INTO users (id, name, role, created_at, updated_at)
VALUES
    (1,'creator1','CREATOR', NOW(), NOW()),
    (2,'student1','STUDENT', NOW(), NOW()),
    (3,'student2','STUDENT', NOW(), NOW()),
    (4,'student3','STUDENT', NOW(), NOW()),
    (5,'creator2','CREATOR', NOW(), NOW());