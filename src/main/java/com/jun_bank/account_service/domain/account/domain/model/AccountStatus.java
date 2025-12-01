package com.jun_bank.account_service.domain.account.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 계좌 상태
 * <p>
 * 계좌의 현재 상태와 각 상태별 허용 작업을 정의합니다.
 *
 * <h3>상태 전이 규칙:</h3>
 * <pre>
 *                      1년간 미거래
 * ┌────────┐ ─────────────────────────▶ ┌─────────┐
 * │ ACTIVE │                             │ DORMANT │
 * └────────┘ ◀───────────────────────── └─────────┘
 *     │             휴면 해제                 │
 *     │                                      │
 *     │ 동결 (관리자)                   동결 (관리자)
 *     ▼                                      ▼
 * ┌────────┐                            ┌────────┐
 * │ FROZEN │                            │ FROZEN │
 * └────────┘                            └────────┘
 *     │                                      │
 *     │ 해제 (관리자)                  해제 (관리자)
 *     ▼                                      ▼
 * ┌────────┐                            ┌─────────┐
 * │ ACTIVE │                            │ DORMANT │
 * └────────┘                            └─────────┘
 *     │
 *     │ 해지 (잔액 0)
 *     ▼
 * ┌────────┐
 * │ CLOSED │  ← 최종 상태 (되돌릴 수 없음)
 * └────────┘
 * </pre>
 *
 * <h3>상태별 허용 작업:</h3>
 * <table border="1">
 *   <tr><th>상태</th><th>입금</th><th>출금</th><th>조회</th><th>해지</th></tr>
 *   <tr><td>ACTIVE</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr>
 *   <tr><td>DORMANT</td><td>✓</td><td>✗</td><td>✓</td><td>✓</td></tr>
 *   <tr><td>FROZEN</td><td>✗</td><td>✗</td><td>✓</td><td>✗</td></tr>
 *   <tr><td>CLOSED</td><td>✗</td><td>✗</td><td>✓</td><td>✗</td></tr>
 * </table>
 *
 * @see Account
 */
public enum AccountStatus {

    /**
     * 정상 상태
     * <p>
     * 모든 거래가 가능한 정상 상태입니다.
     * </p>
     */
    ACTIVE("정상", true, true, true),

    /**
     * 휴면 상태
     * <p>
     * 1년 이상 거래가 없어 휴면 처리된 상태입니다.
     * 입금은 가능하나 출금은 휴면 해제 후 가능합니다.
     * </p>
     */
    DORMANT("휴면", true, false, true),

    /**
     * 동결 상태
     * <p>
     * 관리자에 의해 동결된 상태입니다.
     * 사기, 법적 분쟁 등의 사유로 동결됩니다.
     * 모든 입출금이 불가하며 관리자만 해제 가능합니다.
     * </p>
     */
    FROZEN("동결", false, false, false),

    /**
     * 해지 상태
     * <p>
     * 계좌가 해지된 최종 상태입니다.
     * 잔액이 0인 경우에만 해지 가능합니다.
     * 되돌릴 수 없는 최종 상태입니다.
     * </p>
     */
    CLOSED("해지", false, false, false);

    private final String description;
    private final boolean canDeposit;
    private final boolean canWithdraw;
    private final boolean canClose;

    AccountStatus(String description, boolean canDeposit, boolean canWithdraw, boolean canClose) {
        this.description = description;
        this.canDeposit = canDeposit;
        this.canWithdraw = canWithdraw;
        this.canClose = canClose;
    }

    /**
     * 상태 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 입금 가능 여부 확인
     *
     * @return 입금 가능하면 true
     */
    public boolean canDeposit() {
        return canDeposit;
    }

    /**
     * 출금 가능 여부 확인
     *
     * @return 출금 가능하면 true
     */
    public boolean canWithdraw() {
        return canWithdraw;
    }

    /**
     * 해지 가능 여부 확인
     *
     * @return 해지 가능하면 true
     */
    public boolean canClose() {
        return canClose;
    }

    /**
     * 거래 가능 여부 확인
     * <p>
     * 입금 또는 출금이 가능한지 확인합니다.
     * </p>
     *
     * @return 거래 가능하면 true
     */
    public boolean canTransact() {
        return canDeposit || canWithdraw;
    }

    /**
     * 활성 상태 여부 확인
     *
     * @return ACTIVE이면 true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 휴면 상태 여부 확인
     *
     * @return DORMANT이면 true
     */
    public boolean isDormant() {
        return this == DORMANT;
    }

    /**
     * 동결 상태 여부 확인
     *
     * @return FROZEN이면 true
     */
    public boolean isFrozen() {
        return this == FROZEN;
    }

    /**
     * 해지 상태 여부 확인
     *
     * @return CLOSED이면 true
     */
    public boolean isClosed() {
        return this == CLOSED;
    }

    /**
     * 특정 상태로 전환 가능 여부 확인
     * <p>
     * 같은 상태로의 전환은 불가능합니다.
     * CLOSED 상태는 최종 상태로 다른 상태로 전환 불가합니다.
     * </p>
     *
     * @param target 전환하려는 상태
     * @return 전환 가능하면 true
     */
    public boolean canTransitionTo(AccountStatus target) {
        if (this == target) {
            return false;
        }
        return getAllowedTransitions().contains(target);
    }

    /**
     * 현재 상태에서 전환 가능한 상태 목록 반환
     *
     * @return 전환 가능한 상태 Set
     */
    public Set<AccountStatus> getAllowedTransitions() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(DORMANT, FROZEN, CLOSED);
            case DORMANT -> EnumSet.of(ACTIVE, FROZEN, CLOSED);
            case FROZEN -> EnumSet.of(ACTIVE, DORMANT);  // 관리자만 해제 가능
            case CLOSED -> EnumSet.noneOf(AccountStatus.class);  // 최종 상태
        };
    }
}