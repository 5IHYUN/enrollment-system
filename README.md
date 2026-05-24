# 프로젝트 개요
### BE-A. 수강 신청 시스템

크리에이터가 강의를 개설하고,
수강생이 강의를 신청/결제/취소할 수 있는 수강 신청 시스템입니다.

정원 제한, 상태 전이, 동시성 제어 등의 비즈니스 규칙을 구현했습니다.
---
# 기술 스택
![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-384D54?style=for-the-badge&logo=docker&logoColor=white)
---
# 실행 방법
### Docker 실행

```
docker compose up --build
```

실행 후:

- App: http://localhost:8080
- MySQL: localhost:3307

기본 테스트 유저:

| id | name | role |
| --- | --- | --- |
| 1 | creator1 | CREATOR |
| 2 | student1 | STUDENT |
| 3 | student2 | STUDENT |
| 4 | student3 | STUDENT |
| 5 | creator2 | CREATOR |
---
# 데이터 모델 설명 (ERD)
### ERD
<img width="764" height="1098" alt="image" src="https://github.com/user-attachments/assets/2940057d-b9f4-446e-a3d0-6707baf79ba2" />
### 관계 설명

#### users ↔ classes

- 한 명의 크리에이터는 여러 개의 강의를 생성할 수 있습니다.
- 하나의 강의는 한 명의 크리에이터에 의해 생성됩니다.
- 관계: `users 1 : N classes`
- FK: `classes.creator_id → users.id`

#### users ↔ enrollments

- 한 명의 수강생은 여러 강의에 수강 신청할 수 있습니다.
- 하나의 수강 신청 내역은 한 명의 사용자에게 속합니다.
- 관계: `users 1 : N enrollments`
- FK: `enrollments.user_id → users.id`

#### classes ↔ enrollments

- 하나의 강의에는 여러 수강 신청 내역이 존재할 수 있습니다.
- 하나의 수강 신청 내역은 하나의 강의에 속합니다.
- 관계: `classes 1 : N enrollments`
- FK: `enrollments.class_id → classes.id`

### 제약 조건

- 동일한 사용자가 같은 강의에 중복 신청하지 못하도록 `user_id`, `class_id`에 유니크 제약을 설정했습니다.
- 취소된 신청은 새 row를 생성하지 않고 기존 row의 상태를 `PENDING`으로 복구하여 재신청을 처리합니다.
---
# 요구사항 해석 및 가정
크리에이터가 강의를 개설하고, 수강생이 강의를 신청한 뒤 결제 확정을 통해 수강 상태가 변경되는 흐름을 기준으로 구현했습니다.

### 사용자 역할

프로젝트에서는 사용자를 `CREATOR`, `STUDENT` 두 역할로 구분.

- CREATOR
    - 강의 생성 가능
    - 강의 상태 변경 가능
    - 자신이 생성한 강의의 수강생 목록 조회 가능
- STUDENT
    - 강의 조회 가능
    - 수강 신청 / 결제 확정 / 취소 가능

인증/인가는 실제 로그인 방식 대신 `X-USER-ID` 헤더로 사용자를 식별하는 방식으로 단순화했습니다.

### 강의 상태 흐름

- **DRAFT**
    - 강의 생성 직후 상태
    - 수강 신청 불가
- **OPEN**
    - 수강 신청 가능 상태
- **CLOSED**
    - 수강 마감 상태
    - 더 이상 신청 불가

CLOSED 상태의 강의는 다시 OPEN 상태로 변경할 수 없도록 제한했습니다.

### 수강 신청 상태 흐름

- PENDING
    - 수강 신청은 완료했지만 결제 확정 전인 상태
- CONFIRMED
    - 결제가 완료되어 수강이 확정된 상태
- CANCELLED
    - 사용자가 수강을 취소한 상태

### 현재 신청 인원 기준

 `PENDING`과 `CONFIRMED` 상태를 모두 현재 신청 인원으로 간주.

```
현재 신청 인원 = PENDING + CONFIRMED
```

이는 수강 신청 시점에 자리를 선점한다고 해석했기 때문입니다.

따라서 결제 확정 전이라도 `PENDING` 상태의 신청은 정원 계산에 포함합니다.

### 수강 취소 기준

- `PENDING`
    - 결제 전 상태이므로 언제든 취소 가능
- `CONFIRMED`
    - 결제 완료 상태이므로 결제 후 7일 이내만 취소 가능

결제 후 일정 기간이 지난 신청은 운영 정책상 취소가 불가능하다고 가정했습니다.

### 재신청 기준

취소된 신청은 다시 신청할 수 있다고 해석.

단, 동일 사용자가 같은 강의에 대해 여러 개의 신청 내역을 가지는 것은 중복 신청으로 보았습니다..

따라서 취소된 신청이 있는 경우에는 새로운 신청 내역을 추가하기보다 기존 신청 상태를 다시 신청 가능한 상태로 되돌리는 흐름으로 해석했습니다.
---
# 설계 결정과 이유
### 현재 신청 인원을 컬럼으로 저장하지 않은 이유

현재 신청 인원을 별도의 컬럼으로 저장하지 않고, `PENDING`과 `CONFIRMED` 상태의 신청 내역을 조회해 계산하도록 구현했습니다.

별도 count 컬럼을 관리할 경우 실제 신청 데이터와 count 값이 서로 달라질 가능성이 있다고 판단했습니다.

특히 신청, 결제 확정, 취소 과정에서 count 값을 함께 수정해야 하므로 상태 불일치 위험이 증가할 수 있다고 보았습니다.

따라서 현재 신청 인원은 enrollment 데이터를 기준으로 매번 계산하도록 구현했습니다.

### 취소 후 재신청을 기존 row 복구로 처리한 이유

수강 취소 후 다시 신청하는 경우 새로운 row를 생성하지 않고, 기존 신청 내역의 상태를 `CANCELLED → PENDING`으로 변경하도록 구현했습니다.

선택한 이유는 다음과 같습니다.

- 신청 이력을 유지할 수 있음.
- 동일 데이터의 중복 생성을 방지.
- `(user_id, class_id)` unique constraint 충돌을 피할 수 있음.
- 상태 전이 흐름을 일관되게 유지할 수 있음.

그러나 이 방식은 취소 후 다시 신청했다는 별도의 이력 로그를 남기기 어렵다는 단점이 있습니다.

실제 서비스에서는 별도 신청 이력 테이블이나 이벤트 로그를 통해 상태 변경 기록을 관리할 수 있지만 현재 과제 범위에서는 구조 복잡도를 줄이고 핵심 비즈니스 로직 구현에 집중하기 위해 기존 row 복구 방식을 선택했습니다.

### 비관적 락을 사용한 이유

수강 신청은 정원 제한이 존재하기 때문에 여러 사용자가 동시에 마지막 자리에 신청하는 상황이 발생할 수 있습니다.

동시에 여러 요청이 들어오는 경우 단순 count 조회만으로는 정원 초과 신청이 발생할 가능성이 있다고 판단했습니다.

이를 방지하기 위해 수강 신청 시 강의 row를 조회할 때 비관적 락(Pessimistic Lock)을 사용했습니다.

이를 통해 하나의 트랜잭션이 신청 처리를 완료할 때까지 다른 요청은 대기하도록 구성했습니다.

이번 프로젝트에서는 수강 신청 과정에서 충돌 가능성이 높은 상황(마지막 자리 신청)을 우선적으로 안전하게 처리하는 것이 중요하다고 판단했습니다.

낙관적 락(Optimistic Lock)의 경우 충돌 발생 시 재시도 로직이나 예외 처리 흐름이 추가로 필요하며, 동시에 여러 요청이 발생하는 상황에서는 반복 실패가 발생할 수 있다고 보았습니다.

반면 비관적 락은 DB 레벨에서 row를 선점하기 때문에 구현 흐름이 비교적 단순하며, 정원 초과 방지라는 요구사항을 보다 명확하게 보장할 수 있다고 판단했습니다.

### Docker Compose를 사용한 이유

실행 환경 차이로 인한 문제를 줄이기 위해 Docker Compose 기반 실행 환경을 구성했습니다.
---
# API 목록 및 예시
### 강의 API

| 기능 | Method | URL | Header |
| --- | --- | --- | --- |
| 강의 생성 | POST | `/api/classes` | `X-USER-ID: {creatorId}` |
| 강의 목록 조회 | GET | `/api/classes` | - |
| 강의 상세 조회 | GET | `/api/classes/{classId}` | - |
| 강의 상태 변경 | PATCH | `/api/classes/{classId}/status` | `X-USER-ID: {creatorId}` |
| 강의별 수강생 조회 | GET | `/api/classes/{classId}/students` | `X-USER-ID: {creatorId}` |

### 강의 생성 요청 예시

```
{
  "title":"Spring Boot 입문",
  "description":"스프링 부트 기초 강의입니다.",
  "price":50000,
  "capacity":30,
  "startDate":"2026-06-01",
  "endDate":"2026-06-30"
}
```

### 강의 상태 변경 요청 예시

```
{
  "status":"OPEN"
}
```

사용 가능한 상태값:

```
DRAFT, OPEN, CLOSED
```

### 수강 신청 API

| 기능 | Method | URL | Header |
| --- | --- | --- | --- |
| 수강 신청 | POST | `/api/enrollments/classes/{classId}` | `X-USER-ID: {studentId}` |
| 결제 확정 | PATCH | `/api/enrollments/{enrollmentId}/confirm` | `X-USER-ID: {studentId}` |
| 수강 취소 | PATCH | `/api/enrollments/{enrollmentId}/cancel` | `X-USER-ID: {studentId}` |
| 내 수강 신청 목록 조회 | GET | `/api/enrollments/me` | `X-USER-ID: {studentId}` |
---
# 테스트 실행 방법
### 테스트 구성

### 1. Service 단위 테스트

`CourseServiceTest`, `EnrollmentServiceTest`에서는 Mockito를 사용해 Service 계층의 비즈니스 로직을 검증했습니다.

주요 검증 항목은 다음과 같습니다.

- 강의 생성 성공 / 실패
- 크리에이터 권한 검증
- 강의 기간 검증
- 강의 상태 변경 검증
- 수강 신청 성공
- 중복 신청 실패
- 정원 초과 실패
- 결제 확정 성공 / 실패
- 수강 취소 성공 / 실패
- 결제 확정 후 7일 초과 취소 실패
- 취소 후 재신청 시 기존 신청 내역 복구

**CourseServiceTest 실행**

```java
./mvnw test -Dtest=CourseServiceTest
```

**EnrollmentServiceTest 실행**

```java
./mvnw test -Dtest=EnrollmentServiceTest
```

### 2. 동시성 통합 테스트

`EnrollmentConcurrencyTest`에서는 실제 MySQL DB를 사용해 동시성 상황을 검증했습니다.

정원이 1명인 강의에 여러 사용자가 동시에 수강 신청했을 때, 최종 신청 인원이 정원을 초과하지 않는지 확인했습니다.

동시성 테스트는 실제 DB 락 동작을 확인해야 하므로 Docker MySQL 컨테이너가 실행된 상태에서 테스트하는 것을 기준으로 했습니다.

```
docker compose up -d mysql
./mvnw test -Dtest=EnrollmentConcurrencyTest
```

### 테스트 결과 확인

테스트가 모두 성공하면 다음과 같이 `BUILD SUCCESS`가 출력됩니다.

```
BUILD SUCCESS
```

### 참고: postman 테스트 흐름

[POSTMAN 테스트 흐름](https://www.notion.so/POSTMAN-36645b9050f280cdb39bda6b037e831f?pvs=21)
---
# 미구현 / 제약사항
### 대기열(waitlist) 기능 미구현

정원이 초과된 경우 자동 대기열 등록 기능은 구현하지 않았다.

### 신청 내역 페이지네이션 미구현

신청 내역 조회 시 페이지네이션 기능은 구현하지 않았다.

### 단일 서버 환경 기준 구현

현재 구현은 단일 애플리케이션 서버 환경을 기준으로 작성했다.

분산 환경에서는 Redis 분산 락 등의 추가 고려가 필요할 수 있다.

### Docker 기반 로컬 실행 환경 사용

실행 편의를 위해 Docker Compose 기반 로컬 환경으로 구성했다.

운영 환경 수준의 Secret 관리 및 배포 환경은 포함하지 않았다.
---
# AI 활용 범위

Chat gpt 를 활용해 기본적인 문서 작업, 요구사항 분석, 설계, 테스트 시나리오, 예외 처리, docker 환경 구성 과정에서 도움을 받았습니다.

또한 일부 코드 구현 과정에서도 아이디어와 예시 코드를 참고했으며 모든 내용은 과제 요구사항에 맞게 직접 수정하고 테스트 했습니다.
