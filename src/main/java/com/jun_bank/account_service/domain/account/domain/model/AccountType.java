package com.jun_bank.account_service.domain.account.domain.model;

import java.math.BigDecimal;

/**
 * 계좌 유형
 * <p>
 * 계좌의 종류와 각 유형별 정책을 정의합니다.
 *
 * <h3>계좌 유형별 특성:</h3>
 * <table border="1">
 *   <tr><th>유형</th><th>최소 잔액</th><th>일일 출금 한도</th><th>이자</th><th>중도해지</th></tr>
 *   <tr><td>CHECKING</td><td>0원</td><td>5,000만원</td><td>0.1%</td><td>가능</td></tr>
 *   <tr><td>SAVINGS</td><td>0원</td><td>1,000만원</td><td>2.0%</td><td>가능</td></tr>
 *   <tr><td>DEPOSIT</td><td>100만원</td><td>불가</td><td>3.5%</td><td>불이익</td></tr>
 * </table>
 *
 * <h3>계좌번호 프리픽스:</h3>
 * <ul>
 *   <li>CHECKING: 110 (입출금)</li>
 *   <li>SAVINGS: 220 (저축)</li>
 *   <li>DEPOSIT: 330 (정기예금)</li>
 * </ul>
 *
 * @see Account
 */
public enum AccountType {

    /**
     * 입출금 통장
     * <p>
     * 일상적인 입출금에 사용되는 기본 계좌입니다.
     * 출금/이체가 자유롭고 일일 한도가 가장 높습니다.
     * </p>
     */
    CHECKING(
            "입출금 통장",
            "110",
            BigDecimal.ZERO,                          // 최소 잔액: 0원
            new BigDecimal("50000000"),               // 일일 출금 한도: 5,000만원
            new BigDecimal("0.001"),                  // 이자율: 0.1%
            true,                                     // 자유 출금 가능
            BigDecimal.ZERO                           // 최소 개설 금액: 0원
    ),

    /**
     * 저축 통장
     * <p>
     * 저축 목적의 계좌로 입출금 통장보다 높은 이자를 제공합니다.
     * 출금 한도가 상대적으로 낮아 저축을 유도합니다.
     * </p>
     */
    SAVINGS(
            "저축 통장",
            "220",
            BigDecimal.ZERO,                          // 최소 잔액: 0원
            new BigDecimal("10000000"),               // 일일 출금 한도: 1,000만원
            new BigDecimal("0.02"),                   // 이자율: 2.0%
            true,                                     // 자유 출금 가능
            BigDecimal.ZERO                           // 최소 개설 금액: 0원
    ),

    /**
     * 정기 예금
     * <p>
     * 높은 이자를 제공하는 정기 예금 상품입니다.
     * 만기 전 출금 시 이자 손실이 발생합니다.
     * </p>
     */
    DEPOSIT(
            "정기 예금",
            "330",
            new BigDecimal("1000000"),                // 최소 잔액: 100만원
            BigDecimal.ZERO,                          // 일일 출금 한도: 0 (출금 불가)
            new BigDecimal("0.035"),                  // 이자율: 3.5%
            false,                                    // 자유 출금 불가
            new BigDecimal("1000000")                 // 최소 개설 금액: 100만원
    );

    private final String description;
    private final String accountNumberPrefix;
    private final BigDecimal minimumBalance;
    private final BigDecimal dailyWithdrawalLimit;
    private final BigDecimal interestRate;
    private final boolean allowFreeWithdrawal;
    private final BigDecimal minimumOpeningDeposit;

    AccountType(String description, String accountNumberPrefix, BigDecimal minimumBalance,
                BigDecimal dailyWithdrawalLimit, BigDecimal interestRate,
                boolean allowFreeWithdrawal, BigDecimal minimumOpeningDeposit) {
        this.description = description;
        this.accountNumberPrefix = accountNumberPrefix;
        this.minimumBalance = minimumBalance;
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
        this.interestRate = interestRate;
        this.allowFreeWithdrawal = allowFreeWithdrawal;
        this.minimumOpeningDeposit = minimumOpeningDeposit;
    }

    /**
     * 계좌 유형 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 계좌번호 프리픽스 반환
     * <p>
     * 계좌번호 생성 시 맨 앞 3자리로 사용됩니다.
     * </p>
     *
     * @return 3자리 프리픽스 (예: "110")
     */
    public String getAccountNumberPrefix() {
        return accountNumberPrefix;
    }

    /**
     * 최소 유지 잔액 반환
     *
     * @return 최소 잔액
     */
    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    /**
     * 일일 출금 한도 반환
     *
     * @return 일일 출금 한도 (0이면 출금 불가)
     */
    public BigDecimal getDailyWithdrawalLimit() {
        return dailyWithdrawalLimit;
    }

    /**
     * 연 이자율 반환
     *
     * @return 이자율 (예: 0.02 = 2%)
     */
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    /**
     * 최소 개설 금액 반환
     *
     * @return 계좌 개설 시 최소 입금액
     */
    public BigDecimal getMinimumOpeningDeposit() {
        return minimumOpeningDeposit;
    }

    /**
     * 자유 출금 가능 여부 확인
     * <p>
     * 정기예금의 경우 false를 반환합니다.
     * </p>
     *
     * @return 자유 출금 가능하면 true
     */
    public boolean allowsFreeWithdrawal() {
        return allowFreeWithdrawal;
    }

    /**
     * 입출금 계좌 여부 확인
     *
     * @return CHECKING이면 true
     */
    public boolean isChecking() {
        return this == CHECKING;
    }

    /**
     * 저축 계좌 여부 확인
     *
     * @return SAVINGS이면 true
     */
    public boolean isSavings() {
        return this == SAVINGS;
    }

    /**
     * 정기예금 여부 확인
     *
     * @return DEPOSIT이면 true
     */
    public boolean isDeposit() {
        return this == DEPOSIT;
    }

    /**
     * 특정 금액으로 출금 가능 여부 확인
     * <p>
     * 일일 출금 한도 내에서 출금 가능한지 확인합니다.
     * </p>
     *
     * @param amount 출금 요청 금액
     * @param dailyUsedAmount 오늘 이미 출금한 금액
     * @return 출금 가능하면 true
     */
    public boolean canWithdraw(BigDecimal amount, BigDecimal dailyUsedAmount) {
        if (!allowFreeWithdrawal) {
            return false;
        }
        BigDecimal totalAfterWithdrawal = dailyUsedAmount.add(amount);
        return totalAfterWithdrawal.compareTo(dailyWithdrawalLimit) <= 0;
    }

    /**
     * 초기 입금액이 최소 개설 금액을 충족하는지 확인
     *
     * @param initialDeposit 초기 입금액
     * @return 충족하면 true
     */
    public boolean isValidInitialDeposit(BigDecimal initialDeposit) {
        return initialDeposit.compareTo(minimumOpeningDeposit) >= 0;
    }
}