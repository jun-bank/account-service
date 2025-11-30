# Account Service

> 계좌 관리 서비스 - 계좌 생성, 조회, 잔액 변경, 해지

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8081 |
| 데이터베이스 | account_db (PostgreSQL) |
| 주요 역할 | 계좌 생명주기 및 잔액 관리 |

## 🎯 학습 포인트

### 1. 동시성 제어 ⭐ (핵심 학습 주제)

#### 낙관적 락 (Optimistic Lock)
```java
@Entity
public class Account {
    @Version
    private Long version;  // 버전 필드
}

// 동시 수정 시 OptimisticLockException 발생
// 재시도 로직으로 해결
```

**사용 시점**: 충돌이 적은 경우 (읽기 많음)

```
┌─────────────────────────────────────────────────────────────┐
│                    낙관적 락 동작 방식                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Transaction A                    Transaction B            │
│       │                                │                    │
│       │  1. SELECT (version=1)         │                    │
│       │                                │  2. SELECT (v=1)   │
│       │                                │                    │
│       │  3. UPDATE (v=1→2) ✓           │                    │
│       │                                │                    │
│       │                                │  4. UPDATE (v=1→2) │
│       │                                │     ❌ 실패!        │
│       │                                │     (version 불일치)│
│       │                                │                    │
│       │                                │  5. 재시도 (v=2)   │
│       │                                │     → 성공 ✓       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 비관적 락 (Pessimistic Lock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Optional<Account> findByIdWithLock(@Param("id") Long id);
```

**사용 시점**: 충돌이 잦은 경우 (잔액 변경)

```
┌─────────────────────────────────────────────────────────────┐
│                    비관적 락 동작 방식                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Transaction A                    Transaction B            │
│       │                                │                    │
│       │  1. SELECT FOR UPDATE          │                    │
│       │     🔒 락 획득                  │                    │
│       │                                │  2. SELECT FOR     │
│       │                                │     UPDATE         │
│       │                                │     ⏳ 대기중...    │
│       │  3. UPDATE                     │                    │
│       │  4. COMMIT                     │                    │
│       │     🔓 락 해제                  │                    │
│       │                                │                    │
│       │                                │  5. 🔒 락 획득     │
│       │                                │  6. UPDATE         │
│       │                                │  7. COMMIT ✓       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. 계좌번호 생성 전략
- 체크섬 (Luhn 알고리즘) 적용
- 중복 방지 (Unique 제약)

### 3. 잔액 불변성
- 잔액 직접 수정 금지
- 입금/출금 메서드를 통해서만 변경

---

## 🗄️ 도메인 모델

### Account Entity

```
┌─────────────────────────────────────────────┐
│                  Account                     │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ accountNumber: String (Unique, 14자리)       │
│ userId: Long (FK → User)                    │
│ accountType: AccountType                    │
│ balance: BigDecimal (잔액)                   │
│ dailyWithdrawalAmount: BigDecimal           │
│ status: AccountStatus                       │
│ createdAt: LocalDateTime                    │
│ updatedAt: LocalDateTime                    │
│ version: Long (@Version - 낙관적 락)         │
└─────────────────────────────────────────────┘
```

### AccountType Enum
```java
public enum AccountType {
    CHECKING,   // 입출금 통장
    SAVINGS,    // 저축 통장
    DEPOSIT     // 정기 예금
}
```

### AccountStatus Enum
```java
public enum AccountStatus {
    ACTIVE,     // 정상
    DORMANT,    // 휴면
    FROZEN,     // 동결
    CLOSED      // 해지
}
```

---

## 📡 API 명세

### 1. 계좌 개설
```http
POST /api/v1/accounts
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "accountType": "CHECKING",
  "initialDeposit": 10000
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "accountNumber": "110-1234-5678-90",
  "accountType": "CHECKING",
  "balance": 10000,
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

**이벤트 발행**: `account.created`

---

### 2. 계좌 조회 (단건)
```http
GET /api/v1/accounts/{accountId}
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "id": 1,
  "accountNumber": "110-1234-5678-90",
  "accountType": "CHECKING",
  "balance": 150000,
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 3. 내 계좌 목록 조회
```http
GET /api/v1/accounts?status=ACTIVE
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accounts": [
    {
      "id": 1,
      "accountNumber": "110-1234-5678-90",
      "accountType": "CHECKING",
      "balance": 150000,
      "status": "ACTIVE"
    },
    {
      "id": 2,
      "accountNumber": "110-9876-5432-10",
      "accountType": "SAVINGS",
      "balance": 500000,
      "status": "ACTIVE"
    }
  ],
  "totalBalance": 650000
}
```

---

### 4. 잔액 조회
```http
GET /api/v1/accounts/{accountId}/balance
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "balance": 150000,
  "availableBalance": 150000,
  "dailyWithdrawalLimit": 50000000,
  "dailyWithdrawalUsed": 30000
}
```

---

### 5. 입금 (잔액 증가) - 내부 API
```http
POST /api/v1/accounts/{accountId}/deposit
X-Internal-Service: transaction-service
Content-Type: application/json

{
  "amount": 50000,
  "transactionId": "txn-uuid-1234",
  "description": "급여 입금"
}
```

**Response (200 OK)**
```json
{
  "accountId": 1,
  "previousBalance": 150000,
  "amount": 50000,
  "currentBalance": 200000,
  "transactionId": "txn-uuid-1234"
}
```

**이벤트 발행**: `account.balance.changed`

---

### 6. 출금 (잔액 감소) - 내부 API
```http
POST /api/v1/accounts/{accountId}/withdraw
X-Internal-Service: transaction-service
Content-Type: application/json

{
  "amount": 30000,
  "transactionId": "txn-uuid-5678",
  "description": "ATM 출금"
}
```

**Response (200 OK)**
```json
{
  "accountId": 1,
  "previousBalance": 200000,
  "amount": 30000,
  "currentBalance": 170000,
  "transactionId": "txn-uuid-5678"
}
```

**실패 시 (400 Bad Request)**
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "잔액이 부족합니다.",
  "currentBalance": 170000,
  "requestedAmount": 200000
}
```

---

### 7. 계좌 해지
```http
DELETE /api/v1/accounts/{accountId}
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "message": "계좌가 해지되었습니다.",
  "accountNumber": "110-1234-5678-90",
  "finalBalance": 0,
  "closedAt": "2024-01-15T15:00:00"
}
```

**이벤트 발행**: `account.closed`

**실패 시 (400 Bad Request)**
```json
{
  "error": "BALANCE_NOT_ZERO",
  "message": "잔액이 있는 계좌는 해지할 수 없습니다.",
  "currentBalance": 50000
}
```

---

### 8. 계좌번호로 조회 (이체 시 수취인 확인)
```http
GET /api/v1/accounts/by-number/{accountNumber}
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-9999-8888-77",
  "ownerName": "홍*동",
  "bankName": "준뱅크",
  "valid": true
}
```

---

## 📂 패키지 구조

```
com.jun_bank.account_service
├── AccountServiceApplication.java
├── global/                          # 전역 설정 레이어
│   ├── config/                      # 설정 클래스
│   │   ├── JpaConfig.java           # JPA Auditing 활성화
│   │   ├── QueryDslConfig.java      # QueryDSL JPAQueryFactory 빈
│   │   ├── KafkaProducerConfig.java # Kafka Producer (멱등성, JacksonJsonSerializer)
│   │   ├── KafkaConsumerConfig.java # Kafka Consumer (수동 ACK, JacksonJsonDeserializer)
│   │   ├── SecurityConfig.java      # Spring Security (헤더 기반 인증)
│   │   ├── FeignConfig.java         # Feign Client 설정
│   │   ├── SwaggerConfig.java       # OpenAPI 문서화
│   │   └── AsyncConfig.java         # 비동기 처리 (ThreadPoolTaskExecutor)
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java      # 공통 엔티티 (Audit, Soft Delete)
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java # JPA Auditing 사용자 정보
│   ├── security/
│   │   ├── UserPrincipal.java       # 인증 사용자 Principal
│   │   ├── HeaderAuthenticationFilter.java # Gateway 헤더 인증 필터
│   │   └── SecurityContextUtil.java # SecurityContext 유틸리티
│   ├── feign/
│   │   ├── FeignErrorDecoder.java   # Feign 에러 → BusinessException 변환
│   │   └── FeignRequestInterceptor.java # 인증 헤더 전파
│   └── aop/
│       └── LoggingAspect.java       # 요청/응답 로깅 AOP
└── domain/
    └── account/                     # Account 도메인
        ├── domain/                  # 순수 도메인 (Entity, VO, Enum)
        ├── application/             # 유스케이스, Port, DTO
        ├── infrastructure/          # Adapter (Out) - Repository, Kafka
        └── presentation/            # Adapter (In) - Controller
```

---

## 🔧 Global 레이어 상세

### Config 설정

| 클래스 | 설명 |
|--------|------|
| `JpaConfig` | JPA Auditing 활성화 (`@EnableJpaAuditing`) |
| `QueryDslConfig` | `JPAQueryFactory` 빈 등록 |
| `KafkaProducerConfig` | 멱등성 Producer (ENABLE_IDEMPOTENCE=true, ACKS=all) |
| `KafkaConsumerConfig` | 수동 ACK (MANUAL_IMMEDIATE), group-id: account-service-group |
| `SecurityConfig` | Stateless 세션, 헤더 기반 인증, CSRF 비활성화 |
| `FeignConfig` | 로깅 레벨 BASIC, 에러 디코더, 요청 인터셉터 |
| `SwaggerConfig` | OpenAPI 3.0 문서화 설정 |
| `AsyncConfig` | ThreadPoolTaskExecutor (core=5, max=10, queue=25) |

### Security 설정

| 클래스 | 설명 |
|--------|------|
| `HeaderAuthenticationFilter` | `X-User-Id`, `X-User-Role`, `X-User-Email` 헤더 → SecurityContext |
| `UserPrincipal` | `UserDetails` 구현체, 인증된 사용자 정보 |
| `SecurityContextUtil` | 현재 사용자 조회 유틸리티 |

### BaseEntity (Soft Delete 지원)

```java
@MappedSuperclass
public abstract class BaseEntity {
    private LocalDateTime createdAt;      // 생성일시 (자동)
    private LocalDateTime updatedAt;      // 수정일시 (자동)
    private String createdBy;             // 생성자 (자동)
    private String updatedBy;             // 수정자 (자동)
    private LocalDateTime deletedAt;      // 삭제일시
    private String deletedBy;             // 삭제자
    private Boolean isDeleted = false;    // 삭제 여부
    
    public void delete(String deletedBy);  // Soft Delete
    public void restore();                 // 복구
}
```

---

## 🔗 서비스 간 통신

### 발행 이벤트 (Kafka Producer)
| 이벤트 | 토픽 | 수신 서비스 | 설명 |
|--------|------|-------------|------|
| ACCOUNT_CREATED | account.created | Ledger | 계좌 생성 기록 |
| BALANCE_CHANGED | account.balance.changed | Ledger | 잔액 변경 기록 |
| ACCOUNT_CLOSED | account.closed | Ledger | 계좌 해지 기록 |
| DEBIT_COMPLETED | transfer.debit.completed | Transfer | 출금 완료 응답 |
| CREDIT_COMPLETED | transfer.credit.completed | Transfer | 입금 완료 응답 |

### 수신 이벤트 (Kafka Consumer) - SAGA 참여자
| 이벤트 | 토픽 | 발신 서비스 | 설명 |
|--------|------|-------------|------|
| DEBIT_REQUESTED | transfer.debit.requested | Transfer | 출금 요청 |
| CREDIT_REQUESTED | transfer.credit.requested | Transfer | 입금 요청 |
| DEBIT_ROLLBACK | transfer.debit.rollback | Transfer | 출금 롤백 (보상) |

### Feign Client 호출
| 대상 서비스 | 용도 | 비고 |
|-------------|------|------|
| User Service | 사용자 존재 확인 | 계좌 개설 시 |

---

## ⚙️ 동시성 제어 설정

### 낙관적 락 재시도 설정
```yaml
account-service:
  optimistic-lock-retry-count: 3
```

```java
@Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
)
public void updateBalance(Long accountId, BigDecimal amount) {
    // ...
}
```

### 비관적 락 타임아웃 설정
```yaml
account-service:
  pessimistic-lock-timeout: 5000
```

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
})
Optional<Account> findByIdWithLock(Long id);
```

---

## 🧪 테스트 시나리오

### 1. 동시성 테스트 (낙관적 락)
```java
@Test
void 동시_잔액_수정_낙관적_락_테스트() throws Exception {
    // Given: 잔액 100,000원인 계좌

    // When: 2개의 스레드가 동시에 50,000원 출금 시도
    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<?> thread1 = executor.submit(() ->
            accountService.withdraw(accountId, 50000));
    Future<?> thread2 = executor.submit(() ->
            accountService.withdraw(accountId, 50000));

    // Then: 하나는 성공, 하나는 재시도 후 잔액 부족으로 실패
    // 최종 잔액: 50,000원
}
```

### 2. 동시성 테스트 (비관적 락)
```java
@Test
void 동시_잔액_수정_비관적_락_테스트() throws Exception {
    // Given: 잔액 100,000원인 계좌

    // When: 10개의 스레드가 동시에 10,000원씩 출금 시도
    ExecutorService executor = Executors.newFixedThreadPool(10);

    // Then: 순차적으로 처리되어 최종 잔액 0원
    // (락 대기로 인해 시간 소요)
}
```

### 3. API 테스트
```bash
# 계좌 개설
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER" \
  -d '{"accountType":"CHECKING","initialDeposit":10000}'

# 잔액 조회
curl http://localhost:8080/api/v1/accounts/1/balance \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER"
```

---

## 📝 구현 체크리스트

- [ ] Entity, Repository 생성
- [ ] 계좌번호 생성기 (Luhn 알고리즘)
- [ ] AccountService 구현
- [ ] **낙관적 락 구현 (@Version)**
- [ ] **비관적 락 구현 (@Lock)**
- [ ] **재시도 로직 (@Retryable)**
- [ ] Controller 구현
- [ ] Kafka Producer 구현
- [ ] Kafka Consumer 구현 (SAGA 참여)
- [ ] 동시성 테스트 코드
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서화 (Swagger)