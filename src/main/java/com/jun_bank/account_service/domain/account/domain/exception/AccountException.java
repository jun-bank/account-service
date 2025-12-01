package com.jun_bank.account_service.domain.account.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 계좌 도메인 예외
 * <p>
 * 계좌 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 * {@link AccountErrorCode}를 기반으로 예외를 생성합니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 팩토리 메서드 사용 (권장)
 * throw AccountException.accountNotFound("ACC-a1b2c3d4");
 * throw AccountException.insufficientBalance(currentBalance, requestedAmount);
 * throw AccountException.dailyWithdrawalLimitExceeded(usedAmount, limitAmount);
 * }</pre>
 *
 * @see AccountErrorCode
 * @see BusinessException
 */
public class AccountException extends BusinessException {

    /**
     * 에러 코드로 예외 생성
     *
     * @param errorCode 계좌 에러 코드
     */
    public AccountException(AccountErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지로 예외 생성
     *
     * @param errorCode 계좌 에러 코드
     * @param detailMessage 상세 메시지
     */
    public AccountException(AccountErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 계좌 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return AccountException 인스턴스
     */
    public static AccountException invalidAccountIdFormat(String id) {
        return new AccountException(AccountErrorCode.INVALID_ACCOUNT_ID_FORMAT, "id=" + id);
    }

    /**
     * 유효하지 않은 계좌번호 형식 예외 생성
     *
     * @param accountNumber 유효하지 않은 계좌번호
     * @return AccountException 인스턴스
     */
    public static AccountException invalidAccountNumberFormat(String accountNumber) {
        return new AccountException(AccountErrorCode.INVALID_ACCOUNT_NUMBER_FORMAT,
                "accountNumber=" + accountNumber);
    }

    /**
     * 유효하지 않은 금액 예외 생성
     * <p>
     * 금액이 0 이하이거나 유효하지 않은 경우 발생합니다.
     * </p>
     *
     * @param amount 유효하지 않은 금액
     * @return AccountException 인스턴스
     */
    public static AccountException invalidAmount(BigDecimal amount) {
        return new AccountException(AccountErrorCode.INVALID_AMOUNT,
                "amount=" + (amount != null ? amount.toPlainString() : "null"));
    }

    /**
     * 유효하지 않은 초기 입금액 예외 생성
     *
     * @param amount 입력된 초기 입금액
     * @param minDeposit 최소 입금액
     * @return AccountException 인스턴스
     */
    public static AccountException invalidInitialDeposit(BigDecimal amount, BigDecimal minDeposit) {
        return new AccountException(AccountErrorCode.INVALID_INITIAL_DEPOSIT,
                String.format("입력=%s, 최소=%s", amount.toPlainString(), minDeposit.toPlainString()));
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    /**
     * 계좌를 찾을 수 없음 예외 생성
     *
     * @param accountId 찾을 수 없는 계좌 ID
     * @return AccountException 인스턴스
     */
    public static AccountException accountNotFound(String accountId) {
        return new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND,
                "accountId=" + accountId);
    }

    /**
     * 계좌번호로 계좌를 찾을 수 없음 예외 생성
     *
     * @param accountNumber 찾을 수 없는 계좌번호
     * @return AccountException 인스턴스
     */
    public static AccountException accountNotFoundByNumber(String accountNumber) {
        return new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND_BY_NUMBER,
                "accountNumber=" + accountNumber);
    }

    // ========================================
    // 잔액/금액 관련 팩토리 메서드
    // ========================================

    /**
     * 잔액 부족 예외 생성
     * <p>
     * 출금/이체 시 현재 잔액보다 큰 금액을 요청한 경우 발생합니다.
     * </p>
     *
     * @param currentBalance 현재 잔액
     * @param requestedAmount 요청 금액
     * @return AccountException 인스턴스
     */
    public static AccountException insufficientBalance(BigDecimal currentBalance, BigDecimal requestedAmount) {
        return new AccountException(AccountErrorCode.INSUFFICIENT_BALANCE,
                String.format("현재잔액=%s, 요청금액=%s",
                        currentBalance.toPlainString(), requestedAmount.toPlainString()));
    }

    /**
     * 잔액이 0이 아님 (해지 불가) 예외 생성
     *
     * @param currentBalance 현재 잔액
     * @return AccountException 인스턴스
     */
    public static AccountException balanceNotZero(BigDecimal currentBalance) {
        return new AccountException(AccountErrorCode.BALANCE_NOT_ZERO,
                "currentBalance=" + currentBalance.toPlainString());
    }

    /**
     * 입금 금액이 너무 큼 예외 생성
     *
     * @param amount 요청 금액
     * @param maxDeposit 최대 입금 한도
     * @return AccountException 인스턴스
     */
    public static AccountException depositAmountTooLarge(BigDecimal amount, BigDecimal maxDeposit) {
        return new AccountException(AccountErrorCode.DEPOSIT_AMOUNT_TOO_LARGE,
                String.format("요청=%s, 한도=%s", amount.toPlainString(), maxDeposit.toPlainString()));
    }

    /**
     * 출금 금액이 너무 큼 예외 생성
     *
     * @param amount 요청 금액
     * @param maxWithdrawal 최대 출금 한도
     * @return AccountException 인스턴스
     */
    public static AccountException withdrawalAmountTooLarge(BigDecimal amount, BigDecimal maxWithdrawal) {
        return new AccountException(AccountErrorCode.WITHDRAWAL_AMOUNT_TOO_LARGE,
                String.format("요청=%s, 한도=%s", amount.toPlainString(), maxWithdrawal.toPlainString()));
    }

    // ========================================
    // 상태 관련 팩토리 메서드
    // ========================================

    /**
     * 비활성 계좌 예외 생성
     * <p>
     * 휴면, 동결, 해지 상태의 계좌로 거래를 시도한 경우 발생합니다.
     * </p>
     *
     * @param status 현재 상태
     * @return AccountException 인스턴스
     */
    public static AccountException accountNotActive(String status) {
        return new AccountException(AccountErrorCode.ACCOUNT_NOT_ACTIVE, "status=" + status);
    }

    /**
     * 이미 해지된 계좌 예외 생성
     *
     * @return AccountException 인스턴스
     */
    public static AccountException accountAlreadyClosed() {
        return new AccountException(AccountErrorCode.ACCOUNT_ALREADY_CLOSED);
    }

    /**
     * 동결된 계좌 예외 생성
     *
     * @return AccountException 인스턴스
     */
    public static AccountException accountFrozen() {
        return new AccountException(AccountErrorCode.ACCOUNT_FROZEN);
    }

    /**
     * 휴면 계좌 예외 생성
     *
     * @return AccountException 인스턴스
     */
    public static AccountException accountDormant() {
        return new AccountException(AccountErrorCode.ACCOUNT_DORMANT);
    }

    /**
     * 허용되지 않은 상태 전이 예외 생성
     *
     * @param from 현재 상태
     * @param to 요청한 상태
     * @return AccountException 인스턴스
     */
    public static AccountException invalidStatusTransition(String from, String to) {
        return new AccountException(AccountErrorCode.INVALID_STATUS_TRANSITION,
                String.format("from=%s, to=%s", from, to));
    }

    /**
     * 이미 활성 상태 예외 생성
     *
     * @return AccountException 인스턴스
     */
    public static AccountException accountAlreadyActive() {
        return new AccountException(AccountErrorCode.ACCOUNT_ALREADY_ACTIVE);
    }

    // ========================================
    // 한도 관련 팩토리 메서드
    // ========================================

    /**
     * 일일 출금 한도 초과 예외 생성
     *
     * @param usedAmount 오늘 사용한 금액
     * @param limitAmount 일일 한도
     * @param requestedAmount 요청 금액
     * @return AccountException 인스턴스
     */
    public static AccountException dailyWithdrawalLimitExceeded(
            BigDecimal usedAmount, BigDecimal limitAmount, BigDecimal requestedAmount) {
        return new AccountException(AccountErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED,
                String.format("사용=%s, 한도=%s, 요청=%s",
                        usedAmount.toPlainString(), limitAmount.toPlainString(), requestedAmount.toPlainString()));
    }

    /**
     * 일일 이체 한도 초과 예외 생성
     *
     * @param usedAmount 오늘 이체한 금액
     * @param limitAmount 일일 한도
     * @return AccountException 인스턴스
     */
    public static AccountException dailyTransferLimitExceeded(BigDecimal usedAmount, BigDecimal limitAmount) {
        return new AccountException(AccountErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED,
                String.format("사용=%s, 한도=%s", usedAmount.toPlainString(), limitAmount.toPlainString()));
    }

    // ========================================
    // 동시성 관련 팩토리 메서드
    // ========================================

    /**
     * 낙관적 락 충돌 예외 생성
     * <p>
     * 동시에 같은 계좌를 수정하려고 할 때 발생합니다.
     * Application Layer에서 재시도 로직으로 처리해야 합니다.
     * </p>
     *
     * @param accountId 충돌이 발생한 계좌 ID
     * @return AccountException 인스턴스
     */
    public static AccountException optimisticLockConflict(String accountId) {
        return new AccountException(AccountErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "accountId=" + accountId);
    }

    /**
     * 비관적 락 타임아웃 예외 생성
     *
     * @param accountId 타임아웃이 발생한 계좌 ID
     * @return AccountException 인스턴스
     */
    public static AccountException pessimisticLockTimeout(String accountId) {
        return new AccountException(AccountErrorCode.PESSIMISTIC_LOCK_TIMEOUT,
                "accountId=" + accountId);
    }
}