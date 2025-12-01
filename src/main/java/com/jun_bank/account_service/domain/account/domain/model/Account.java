package com.jun_bank.account_service.domain.account.domain.model;

import com.jun_bank.account_service.domain.account.domain.exception.AccountException;
import com.jun_bank.account_service.domain.account.domain.model.vo.AccountId;
import com.jun_bank.account_service.domain.account.domain.model.vo.AccountNumber;
import com.jun_bank.account_service.domain.account.domain.model.vo.Money;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계좌 도메인 모델 (Aggregate Root)
 * <p>
 * 계좌의 핵심 비즈니스 로직을 포함합니다.
 * 잔액 변경은 반드시 {@link #deposit(Money)} 또는 {@link #withdraw(Money)} 메서드를 통해서만 가능합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>잔액 관리 (입금, 출금)</li>
 *   <li>일일 출금 한도 관리</li>
 *   <li>계좌 상태 관리</li>
 *   <li>계좌 유형별 정책 적용</li>
 * </ul>
 *
 * <h3>동시성 제어:</h3>
 * <p>
 * Entity 레이어에서 @Version을 사용하여 낙관적 락을 적용합니다.
 * 도메인 모델에서는 version 필드를 보관하여 충돌 감지를 지원합니다.
 * </p>
 *
 * <h3>일일 한도 관리:</h3>
 * <p>
 * {@code dailyWithdrawalAmount}는 당일 출금 누적액입니다.
 * {@code lastTransactionDate}가 오늘이 아니면 자동으로 0으로 초기화됩니다.
 * </p>
 *
 * <h3>감사 필드 (BaseEntity 매핑):</h3>
 * <ul>
 *   <li>createdAt, updatedAt, createdBy, updatedBy</li>
 *   <li>deletedAt, deletedBy, isDeleted (Soft Delete)</li>
 * </ul>
 *
 * @see AccountType
 * @see AccountStatus
 * @see Money
 */
@Getter
public class Account {

    // ========================================
    // 핵심 필드
    // ========================================

    /**
     * 계좌 ID (시스템 내부 식별자)
     * <p>신규 계좌는 null, 저장 시 생성됩니다.</p>
     */
    private AccountId accountId;

    /**
     * 계좌번호 (사용자에게 노출)
     */
    private AccountNumber accountNumber;

    /**
     * 소유자 User ID (User Service의 USR-xxx)
     */
    private String userId;

    /**
     * 계좌 유형
     */
    private AccountType accountType;

    /**
     * 현재 잔액
     * <p>직접 수정 불가, deposit/withdraw 메서드만 사용</p>
     */
    private Money balance;

    /**
     * 당일 출금 누적액
     * <p>lastTransactionDate가 오늘이 아니면 0으로 초기화</p>
     */
    private Money dailyWithdrawalAmount;

    /**
     * 마지막 거래일
     * <p>일일 한도 초기화 여부 판단에 사용</p>
     */
    private LocalDate lastTransactionDate;

    /**
     * 계좌 상태
     */
    private AccountStatus status;

    /**
     * 버전 (낙관적 락)
     * <p>JPA @Version 필드와 매핑됩니다.</p>
     */
    private Long version;

    // ========================================
    // 감사 필드 (BaseEntity 매핑)
    // ========================================

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;

    /** 생성자 ID */
    private String createdBy;

    /** 수정자 ID */
    private String updatedBy;

    /** 삭제 일시 (Soft Delete) */
    private LocalDateTime deletedAt;

    /** 삭제자 ID (Soft Delete) */
    private String deletedBy;

    /** 삭제 여부 (Soft Delete) */
    private Boolean isDeleted;

    /**
     * private 생성자
     */
    private Account() {}

    // ========================================
    // 생성 메서드 (Builder 패턴)
    // ========================================

    /**
     * 신규 계좌 생성 빌더
     * <p>
     * 계좌 개설 시 사용합니다.
     * accountId는 null, status는 ACTIVE로 초기화됩니다.
     * </p>
     *
     * @return AccountCreateBuilder
     */
    public static AccountCreateBuilder createBuilder() {
        return new AccountCreateBuilder();
    }

    /**
     * DB 복원용 빌더
     *
     * @return AccountRestoreBuilder
     */
    public static AccountRestoreBuilder restoreBuilder() {
        return new AccountRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 신규 여부 확인
     *
     * @return accountId가 null이면 true
     */
    public boolean isNew() {
        return this.accountId == null;
    }

    /**
     * 활성 상태 여부 확인
     *
     * @return ACTIVE이면 true
     */
    public boolean isActive() {
        return this.status.isActive();
    }

    /**
     * 해지 상태 여부 확인
     *
     * @return CLOSED이면 true
     */
    public boolean isClosed() {
        return this.status.isClosed();
    }

    /**
     * 입금 가능 여부 확인
     *
     * @return 입금 가능하면 true
     */
    public boolean canDeposit() {
        return this.status.canDeposit();
    }

    /**
     * 출금 가능 여부 확인
     * <p>
     * 계좌 상태와 계좌 유형 모두 확인합니다.
     * </p>
     *
     * @return 출금 가능하면 true
     */
    public boolean canWithdraw() {
        return this.status.canWithdraw() && this.accountType.allowsFreeWithdrawal();
    }

    /**
     * 잔액이 0인지 확인
     *
     * @return 잔액이 0이면 true
     */
    public boolean hasZeroBalance() {
        return this.balance.isZero();
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 입금
     * <p>
     * 계좌 상태가 입금 가능한 상태인지 확인 후 잔액을 증가시킵니다.
     * </p>
     *
     * @param amount 입금 금액
     * @throws AccountException 입금 불가 상태인 경우
     * @throws AccountException 금액이 유효하지 않은 경우
     */
    public void deposit(Money amount) {
        validateCanDeposit();
        validatePositiveAmount(amount);

        this.balance = this.balance.add(amount);
        updateTransactionDate();
    }

    /**
     * 출금
     * <p>
     * 계좌 상태, 잔액, 일일 한도를 확인 후 잔액을 감소시킵니다.
     * </p>
     *
     * @param amount 출금 금액
     * @throws AccountException 출금 불가 상태인 경우
     * @throws AccountException 잔액 부족인 경우
     * @throws AccountException 일일 한도 초과인 경우
     */
    public void withdraw(Money amount) {
        validateCanWithdraw();
        validatePositiveAmount(amount);
        validateSufficientBalance(amount);
        validateDailyWithdrawalLimit(amount);

        this.balance = this.balance.subtract(amount);
        this.dailyWithdrawalAmount = this.dailyWithdrawalAmount.add(amount);
        updateTransactionDate();
    }

    /**
     * 계좌 해지
     * <p>
     * 잔액이 0인 경우에만 해지 가능합니다.
     * 상태를 CLOSED로 변경합니다.
     * </p>
     *
     * @throws AccountException 이미 해지된 경우
     * @throws AccountException 잔액이 남아있는 경우
     */
    public void close() {
        if (this.status.isClosed()) {
            throw AccountException.accountAlreadyClosed();
        }
        if (!hasZeroBalance()) {
            throw AccountException.balanceNotZero(this.balance.amount());
        }

        this.status = AccountStatus.CLOSED;
    }

    /**
     * 계좌 동결
     * <p>
     * 관리자에 의한 계좌 동결입니다.
     * 모든 거래가 불가능해집니다.
     * </p>
     */
    public void freeze() {
        validateStatusTransition(AccountStatus.FROZEN);
        this.status = AccountStatus.FROZEN;
    }

    /**
     * 휴면 처리
     * <p>
     * 1년 이상 거래가 없는 계좌를 휴면 처리합니다.
     * </p>
     */
    public void toDormant() {
        validateStatusTransition(AccountStatus.DORMANT);
        this.status = AccountStatus.DORMANT;
    }

    /**
     * 활성화
     * <p>
     * 휴면 또는 동결 상태에서 정상 상태로 복구합니다.
     * </p>
     */
    public void activate() {
        if (this.status.isActive()) {
            throw AccountException.accountAlreadyActive();
        }
        validateStatusTransition(AccountStatus.ACTIVE);
        this.status = AccountStatus.ACTIVE;
    }

    // ========================================
    // Private 검증 메서드
    // ========================================

    /**
     * 입금 가능 상태 검증
     */
    private void validateCanDeposit() {
        if (this.status.isClosed()) {
            throw AccountException.accountAlreadyClosed();
        }
        if (this.status.isFrozen()) {
            throw AccountException.accountFrozen();
        }
        if (!this.status.canDeposit()) {
            throw AccountException.accountNotActive(this.status.name());
        }
    }

    /**
     * 출금 가능 상태 검증
     */
    private void validateCanWithdraw() {
        if (this.status.isClosed()) {
            throw AccountException.accountAlreadyClosed();
        }
        if (this.status.isFrozen()) {
            throw AccountException.accountFrozen();
        }
        if (this.status.isDormant()) {
            throw AccountException.accountDormant();
        }
        if (!this.accountType.allowsFreeWithdrawal()) {
            throw AccountException.accountNotActive("정기예금은 자유 출금 불가");
        }
    }

    /**
     * 양수 금액 검증
     */
    private void validatePositiveAmount(Money amount) {
        if (!amount.isPositive()) {
            throw AccountException.invalidAmount(amount.amount());
        }
    }

    /**
     * 잔액 충분 여부 검증
     */
    private void validateSufficientBalance(Money amount) {
        if (this.balance.isLessThan(amount)) {
            throw AccountException.insufficientBalance(this.balance.amount(), amount.amount());
        }
    }

    /**
     * 일일 출금 한도 검증
     * <p>
     * 오늘 날짜가 아니면 dailyWithdrawalAmount를 초기화합니다.
     * </p>
     */
    private void validateDailyWithdrawalLimit(Money amount) {
        resetDailyLimitIfNeeded();

        Money limit = Money.of(this.accountType.getDailyWithdrawalLimit());
        Money totalAfter = this.dailyWithdrawalAmount.add(amount);

        if (totalAfter.isGreaterThan(limit)) {
            throw AccountException.dailyWithdrawalLimitExceeded(
                    this.dailyWithdrawalAmount.amount(),
                    limit.amount(),
                    amount.amount());
        }
    }

    /**
     * 상태 전이 검증
     */
    private void validateStatusTransition(AccountStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw AccountException.invalidStatusTransition(this.status.name(), target.name());
        }
    }

    /**
     * 일일 한도 초기화 (날짜가 바뀌면)
     */
    private void resetDailyLimitIfNeeded() {
        LocalDate today = LocalDate.now();
        if (this.lastTransactionDate == null || !this.lastTransactionDate.equals(today)) {
            this.dailyWithdrawalAmount = Money.ZERO;
        }
    }

    /**
     * 거래일 갱신
     */
    private void updateTransactionDate() {
        this.lastTransactionDate = LocalDate.now();
    }

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * 신규 계좌 생성 빌더
     */
    public static class AccountCreateBuilder {
        private String userId;
        private AccountType accountType;
        private Money initialDeposit = Money.ZERO;

        /**
         * 소유자 ID 설정
         *
         * @param userId USR-xxx 형식
         * @return this
         */
        public AccountCreateBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 계좌 유형 설정
         *
         * @param accountType 계좌 유형
         * @return this
         */
        public AccountCreateBuilder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * 초기 입금액 설정
         *
         * @param initialDeposit 초기 입금액
         * @return this
         */
        public AccountCreateBuilder initialDeposit(Money initialDeposit) {
            this.initialDeposit = initialDeposit;
            return this;
        }

        /**
         * Account 객체 생성
         * <p>
         * 계좌번호를 자동 생성하고 상태를 ACTIVE로 설정합니다.
         * </p>
         *
         * @return 신규 Account 객체
         * @throws AccountException 초기 입금액이 최소 금액 미만인 경우
         */
        public Account build() {
            // 초기 입금액 검증
            if (!accountType.isValidInitialDeposit(initialDeposit.amount())) {
                throw AccountException.invalidInitialDeposit(
                        initialDeposit.amount(),
                        accountType.getMinimumOpeningDeposit());
            }

            Account account = new Account();
            account.userId = this.userId;
            account.accountType = this.accountType;
            account.accountNumber = AccountNumber.of(AccountNumber.generate(this.accountType));
            account.balance = this.initialDeposit;
            account.dailyWithdrawalAmount = Money.ZERO;
            account.lastTransactionDate = LocalDate.now();
            account.status = AccountStatus.ACTIVE;
            account.isDeleted = false;

            return account;
        }
    }

    /**
     * DB 복원용 빌더
     */
    public static class AccountRestoreBuilder {
        private AccountId accountId;
        private AccountNumber accountNumber;
        private String userId;
        private AccountType accountType;
        private Money balance;
        private Money dailyWithdrawalAmount;
        private LocalDate lastTransactionDate;
        private AccountStatus status;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;

        public AccountRestoreBuilder accountId(AccountId accountId) {
            this.accountId = accountId;
            return this;
        }

        public AccountRestoreBuilder accountNumber(AccountNumber accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public AccountRestoreBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public AccountRestoreBuilder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        public AccountRestoreBuilder balance(Money balance) {
            this.balance = balance;
            return this;
        }

        public AccountRestoreBuilder dailyWithdrawalAmount(Money dailyWithdrawalAmount) {
            this.dailyWithdrawalAmount = dailyWithdrawalAmount;
            return this;
        }

        public AccountRestoreBuilder lastTransactionDate(LocalDate lastTransactionDate) {
            this.lastTransactionDate = lastTransactionDate;
            return this;
        }

        public AccountRestoreBuilder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public AccountRestoreBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public AccountRestoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AccountRestoreBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AccountRestoreBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public AccountRestoreBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public AccountRestoreBuilder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public AccountRestoreBuilder deletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
            return this;
        }

        public AccountRestoreBuilder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Account build() {
            Account account = new Account();
            account.accountId = this.accountId;
            account.accountNumber = this.accountNumber;
            account.userId = this.userId;
            account.accountType = this.accountType;
            account.balance = this.balance;
            account.dailyWithdrawalAmount = this.dailyWithdrawalAmount;
            account.lastTransactionDate = this.lastTransactionDate;
            account.status = this.status;
            account.version = this.version;
            account.createdAt = this.createdAt;
            account.updatedAt = this.updatedAt;
            account.createdBy = this.createdBy;
            account.updatedBy = this.updatedBy;
            account.deletedAt = this.deletedAt;
            account.deletedBy = this.deletedBy;
            account.isDeleted = this.isDeleted;
            return account;
        }
    }
}