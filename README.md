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
public class AccountEntity {
    @Version
    private Long version;  // 버전 필드
}

// 동시 수정 시 OptimisticLockException 발생
// 재시도 로직으로 해결
```

**동작 방식:**
```
Transaction A                    Transaction B
    │                                │
    │  1. SELECT (version=1)         │
    │                                │  2. SELECT (v=1)
    │                                │
    │  3. UPDATE (v=1→2) ✓           │
    │                                │
    │                                │  4. UPDATE (v=1→2)
    │                                │     ❌ 실패! (version 불일치)
    │                                │
    │                                │  5. 재시도 (v=2) → 성공 ✓
```

#### 비관적 락 (Pessimistic Lock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
Optional<AccountEntity> findByIdWithLock(String id);
```

**동작 방식:**
```
Transaction A                    Transaction B
    │                                │
    │  1. SELECT FOR UPDATE          │
    │     🔒 락 획득                  │
    │                                │  2. SELECT FOR UPDATE
    │                                │     ⏳ 대기중...
    │  3. UPDATE                     │
    │  4. COMMIT 🔓 해제             │
    │                                │
    │                                │  5. 🔒 락 획득
    │                                │  6. UPDATE → COMMIT ✓
```

### 2. Money VO를 통한 금융 계산
- BigDecimal 기반 정밀 계산
- 불변 객체로 안전한 연산
- 음수 방지, 포맷팅 지원

### 3. 계좌번호 생성 (Luhn 알고리즘)
- 체크섬으로 입력 오류 감지
- 계좌 유형별 프리픽스

---

## 🗄️ 도메인 모델

### 도메인 구조
```
domain/account/domain/
├── exception/
│   ├── AccountErrorCode.java    # 에러 코드 정의
│   └── AccountException.java    # 도메인 예외
└── model/
    ├── Account.java             # 계좌 Aggregate Root
    ├── AccountType.java         # 유형 Enum (정책 메서드)
    ├── AccountStatus.java       # 상태 Enum (정책 메서드)
    └── vo/
        ├── AccountId.java       # 계좌 ID (ACC-xxxxxxxx)
        ├── AccountNumber.java   # 계좌번호 (Luhn 체크섬)
        └── Money.java           # 금액 (BigDecimal 래퍼)
```

### Account 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                         Account                              │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ accountId: AccountId (PK, ACC-xxxxxxxx)                     │
│ accountNumber: AccountNumber (XXX-XXXX-XXXX-XX, Luhn 검증)  │
│ userId: String (소유자, USR-xxx)                            │
│ accountType: AccountType (CHECKING/SAVINGS/DEPOSIT)         │
│ balance: Money (현재 잔액)                                   │
│ dailyWithdrawalAmount: Money (당일 출금 누적)                │
│ lastTransactionDate: LocalDate (일일 한도 초기화용)          │
│ status: AccountStatus (ACTIVE/DORMANT/FROZEN/CLOSED)        │
│ version: Long (낙관적 락)                                    │
├─────────────────────────────────────────────────────────────┤
│ 【감사 필드 - BaseEntity】                                    │
│ createdAt, updatedAt, createdBy, updatedBy                  │
│ deletedAt, deletedBy, isDeleted (Soft Delete)               │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + deposit(Money): void       // 입금 (상태 검증)             │
│ + withdraw(Money): void      // 출금 (잔액, 한도 검증)       │
│ + close(): void              // 해지 (잔액 0 검증)           │
│ + freeze(): void             // 동결 (관리자)                │
│ + toDormant(): void          // 휴면 처리                    │
│ + activate(): void           // 활성화                       │
├─────────────────────────────────────────────────────────────┤
│ 【상태 확인 메서드】                                          │
│ + isNew(): boolean                                          │
│ + isActive(), isClosed()                                    │
│ + canDeposit(), canWithdraw()                               │
│ + hasZeroBalance()                                          │
└─────────────────────────────────────────────────────────────┘
```

### AccountType Enum (계좌 유형 정책)
```java
public enum AccountType {
    CHECKING("입출금", prefix="110", 일일한도=5천만, 이자=0.1%),
    SAVINGS("저축", prefix="220", 일일한도=1천만, 이자=2.0%),
    DEPOSIT("정기예금", prefix="330", 출금불가, 이자=3.5%);
    
    // 정책 메서드
    public boolean allowsFreeWithdrawal();
    public boolean canWithdraw(amount, dailyUsed);
    public boolean isValidInitialDeposit(amount);
}
```

### AccountStatus Enum (상태 정책)
```java
public enum AccountStatus {
    ACTIVE("정상", 입금=✓, 출금=✓, 해지=✓),
    DORMANT("휴면", 입금=✓, 출금=✗, 해지=✓),
    FROZEN("동결", 입금=✗, 출금=✗, 해지=✗),
    CLOSED("해지", 입금=✗, 출금=✗, 해지=✗);
    
    // 정책 메서드
    public boolean canDeposit();
    public boolean canWithdraw();
    public boolean canTransitionTo(target);
}
```

**상태 전이 규칙:**
```
ACTIVE → DORMANT (1년 미거래), FROZEN (관리자), CLOSED (해지)
DORMANT → ACTIVE (해제), FROZEN (관리자), CLOSED (해지)
FROZEN → ACTIVE (관리자 해제), DORMANT
CLOSED → (최종 상태, 전이 불가)
```

### Value Objects

#### AccountNumber (Luhn 알고리즘)
```java
public record AccountNumber(String value) {
    // 형식: XXX-XXXX-XXXX-XX (14자리)
    // 앞 3자리: 유형 프리픽스 (110, 220, 330)
    // 마지막 2자리: Luhn 체크섬
    
    public static String generate(AccountType type);  // 신규 생성
    public String masked();        // "110-****-****-90"
    public String withoutHyphen(); // "11012345678"
}
```

#### Money (금액)
```java
public record Money(BigDecimal amount) implements Comparable<Money> {
    // 0 이상만 허용 (음수 방지)
    // 불변 객체 (연산 결과는 새 객체)
    
    public boolean isGreaterThanOrEqual(Money other);
    public Money add(Money other);
    public Money subtract(Money other);  // 음수 시 예외
    public String formatted();  // "100,000원"
}
```

### Exception 체계

#### AccountErrorCode
```java
public enum AccountErrorCode implements ErrorCode {
    // 유효성 (400)
    INVALID_ACCOUNT_ID_FORMAT, INVALID_ACCOUNT_NUMBER_FORMAT,
    INVALID_AMOUNT, INVALID_INITIAL_DEPOSIT,
    
    // 조회 (404)
    ACCOUNT_NOT_FOUND,
    
    // 잔액/한도 (400)
    INSUFFICIENT_BALANCE, BALANCE_NOT_ZERO,
    DAILY_WITHDRAWAL_LIMIT_EXCEEDED,
    
    // 상태 (422)
    ACCOUNT_NOT_ACTIVE, ACCOUNT_ALREADY_CLOSED,
    ACCOUNT_FROZEN, ACCOUNT_DORMANT,
    
    // 동시성 (409)
    OPTIMISTIC_LOCK_CONFLICT, PESSIMISTIC_LOCK_TIMEOUT;
}
```

#### AccountException (팩토리 메서드)
```java
public class AccountException extends BusinessException {
    public static AccountException accountNotFound(String id);
    public static AccountException insufficientBalance(BigDecimal current, BigDecimal requested);
    public static AccountException dailyWithdrawalLimitExceeded(BigDecimal used, BigDecimal limit, BigDecimal requested);
    public static AccountException optimisticLockConflict(String accountId);
    // ...
}
```

---

## 📡 API 명세

### 1. 계좌 개설
```http
POST /api/v1/accounts
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
Content-Type: application/json

{
  "accountType": "CHECKING",
  "initialDeposit": 10000
}
```

**처리 흐름:**
1. AccountType 정책 검증 (최소 개설 금액)
2. AccountNumber 자동 생성 (Luhn 체크섬)
3. 계좌 저장

**Response (201 Created)**
```json
{
  "accountId": "ACC-a1b2c3d4",
  "accountNumber": "110-1234-5678-90",
  "accountType": "CHECKING",
  "balance": 10000,
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

### 2. 입금 (내부 API)
```http
POST /api/v1/accounts/{accountId}/deposit
X-Internal-Service: transaction-service
Content-Type: application/json

{
  "amount": 50000,
  "transactionId": "TXN-uuid-1234"
}
```

**도메인 검증:**
- `account.canDeposit()` 확인
- `amount > 0` 확인

### 3. 출금 (내부 API)
```http
POST /api/v1/accounts/{accountId}/withdraw
X-Internal-Service: transaction-service
Content-Type: application/json

{
  "amount": 30000,
  "transactionId": "TXN-uuid-5678"
}
```

**도메인 검증:**
- `account.canWithdraw()` 확인
- `balance >= amount` 확인 (잔액 부족 → 400)
- `dailyUsed + amount <= dailyLimit` 확인 (한도 초과 → 400)

### 4. 계좌 해지
```http
DELETE /api/v1/accounts/{accountId}
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

**도메인 검증:**
- `balance == 0` 확인 (잔액 있음 → 400)
- `status != CLOSED` 확인 (이미 해지 → 422)

---

## 📂 패키지 구조

```
com.jun_bank.account_service
├── AccountServiceApplication.java
├── global/                              # 전역 설정 레이어
│   ├── config/
│   │   ├── JpaConfig.java
│   │   ├── QueryDslConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── KafkaConsumerConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── FeignConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── AsyncConfig.java
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java
│   ├── security/
│   │   ├── UserPrincipal.java
│   │   ├── HeaderAuthenticationFilter.java
│   │   └── SecurityContextUtil.java
│   ├── feign/
│   │   ├── FeignErrorDecoder.java
│   │   └── FeignRequestInterceptor.java
│   └── aop/
│       └── LoggingAspect.java
└── domain/
    └── account/                         # Account Bounded Context
        ├── domain/                      # 순수 도메인 ★ 구현 완료
        │   ├── exception/
        │   │   ├── AccountErrorCode.java
        │   │   └── AccountException.java
        │   └── model/
        │       ├── Account.java         # Aggregate Root
        │       ├── AccountType.java     # 유형 Enum (정책)
        │       ├── AccountStatus.java   # 상태 Enum (정책)
        │       └── vo/
        │           ├── AccountId.java
        │           ├── AccountNumber.java  # Luhn 체크섬
        │           └── Money.java          # 금액 VO
        ├── application/                 # 유스케이스 (TODO)
        │   ├── port/
        │   │   ├── in/
        │   │   └── out/
        │   ├── service/
        │   └── dto/
        ├── infrastructure/              # Adapter Out (TODO)
        │   ├── persistence/
        │   │   ├── entity/              # JPA Entity (@Version)
        │   │   ├── repository/
        │   │   └── adapter/
        │   └── kafka/
        └── presentation/                # Adapter In (TODO)
            ├── controller/
            └── dto/
```

---

## 🔗 서비스 간 통신

### Kafka (비동기 이벤트)

**발행:**
| 이벤트 | 토픽 | 수신 서비스 |
|--------|------|-------------|
| ACCOUNT_CREATED | account.created | Ledger |
| BALANCE_CHANGED | account.balance.changed | Ledger |
| ACCOUNT_CLOSED | account.closed | Ledger |
| DEBIT_COMPLETED | transfer.debit.completed | Transfer |
| CREDIT_COMPLETED | transfer.credit.completed | Transfer |

**수신 (SAGA 참여):**
| 이벤트 | 토픽 | 발신 서비스 |
|--------|------|-------------|
| DEBIT_REQUESTED | transfer.debit.requested | Transfer |
| CREDIT_REQUESTED | transfer.credit.requested | Transfer |
| DEBIT_ROLLBACK | transfer.debit.rollback | Transfer |

---

## 📝 구현 체크리스트

### Domain Layer ✅
- [x] AccountErrorCode (에러 코드 정의)
- [x] AccountException (팩토리 메서드 패턴)
- [x] AccountType (유형별 정책)
- [x] AccountStatus (상태별 정책)
- [x] AccountId VO
- [x] AccountNumber VO (Luhn 알고리즘)
- [x] Money VO (BigDecimal 래퍼)
- [x] Account (Aggregate Root, 동시성 지원)

### Application Layer
- [ ] CreateAccountUseCase
- [ ] GetAccountUseCase
- [ ] DepositUseCase
- [ ] WithdrawUseCase
- [ ] CloseAccountUseCase
- [ ] AccountPort (Repository 인터페이스)
- [ ] DTO 정의

### Infrastructure Layer
- [ ] AccountEntity (@Version 낙관적 락)
- [ ] AccountJpaRepository (@Lock 비관적 락)
- [ ] AccountRepositoryAdapter
- [ ] AccountKafkaProducer
- [ ] AccountKafkaConsumer (SAGA 참여)

### Presentation Layer
- [ ] AccountController
- [ ] Request/Response DTO
- [ ] Swagger 문서화

### 테스트
- [ ] 도메인 단위 테스트 (Money, Account)
- [ ] 동시성 테스트 (낙관적/비관적 락)
- [ ] Repository 통합 테스트
- [ ] API 통합 테스트