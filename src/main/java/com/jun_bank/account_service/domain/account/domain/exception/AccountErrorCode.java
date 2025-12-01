package com.jun_bank.account_service.domain.account.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;

/**
 * 계좌 도메인 에러 코드
 * <p>
 * 계좌 관련 비즈니스 로직에서 발생할 수 있는 모든 에러를 정의합니다.
 * common-lib의 {@link ErrorCode} 인터페이스를 구현합니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>ACCOUNT_001~009: 유효성 검증 오류 (400)</li>
 *   <li>ACCOUNT_010~019: 조회 오류 (404)</li>
 *   <li>ACCOUNT_020~029: 잔액/금액 오류 (400)</li>
 *   <li>ACCOUNT_030~039: 상태 오류 (422)</li>
 *   <li>ACCOUNT_040~049: 한도 오류 (400)</li>
 *   <li>ACCOUNT_050~059: 동시성 오류 (409)</li>
 * </ul>
 *
 * @see AccountException
 * @see ErrorCode
 */
public enum AccountErrorCode implements ErrorCode {

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 계좌 ID 형식
     * <p>
     * 계좌 ID가 ACC-xxxxxxxx 형식이 아닌 경우 발생합니다.
     * </p>
     */
    INVALID_ACCOUNT_ID_FORMAT("ACCOUNT_001", "유효하지 않은 계좌 ID 형식입니다", 400),

    /**
     * 유효하지 않은 계좌번호 형식
     * <p>
     * 계좌번호가 XXX-XXXX-XXXX-XX 형식이 아니거나
     * Luhn 체크섬 검증에 실패한 경우 발생합니다.
     * </p>
     */
    INVALID_ACCOUNT_NUMBER_FORMAT("ACCOUNT_002", "유효하지 않은 계좌번호 형식입니다", 400),

    /**
     * 유효하지 않은 계좌 유형
     * <p>
     * 지원하지 않는 계좌 유형인 경우 발생합니다.
     * </p>
     */
    INVALID_ACCOUNT_TYPE("ACCOUNT_003", "유효하지 않은 계좌 유형입니다", 400),

    /**
     * 유효하지 않은 금액
     * <p>
     * 금액이 0 이하이거나 음수인 경우 발생합니다.
     * </p>
     */
    INVALID_AMOUNT("ACCOUNT_004", "유효하지 않은 금액입니다", 400),

    /**
     * 유효하지 않은 초기 입금액
     * <p>
     * 계좌 개설 시 최소 입금액 미달 시 발생합니다.
     * </p>
     */
    INVALID_INITIAL_DEPOSIT("ACCOUNT_005", "최소 입금액 이상을 입금해야 합니다", 400),

    // ========================================
    // 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 계좌를 찾을 수 없음
     * <p>
     * 해당 ID 또는 계좌번호의 계좌가 존재하지 않는 경우 발생합니다.
     * </p>
     */
    ACCOUNT_NOT_FOUND("ACCOUNT_010", "계좌를 찾을 수 없습니다", 404),

    /**
     * 계좌번호에 해당하는 계좌를 찾을 수 없음
     */
    ACCOUNT_NOT_FOUND_BY_NUMBER("ACCOUNT_011", "해당 계좌번호의 계좌를 찾을 수 없습니다", 404),

    // ========================================
    // 잔액/금액 오류 (400 Bad Request)
    // ========================================

    /**
     * 잔액 부족
     * <p>
     * 출금/이체 시 요청 금액이 현재 잔액보다 큰 경우 발생합니다.
     * </p>
     */
    INSUFFICIENT_BALANCE("ACCOUNT_020", "잔액이 부족합니다", 400),

    /**
     * 잔액이 0이 아님 (해지 불가)
     * <p>
     * 계좌 해지 시 잔액이 남아있는 경우 발생합니다.
     * 잔액을 모두 출금한 후 해지해야 합니다.
     * </p>
     */
    BALANCE_NOT_ZERO("ACCOUNT_021", "잔액이 있는 계좌는 해지할 수 없습니다", 400),

    /**
     * 입금 금액이 너무 큼
     * <p>
     * 1회 입금 한도를 초과한 경우 발생합니다.
     * </p>
     */
    DEPOSIT_AMOUNT_TOO_LARGE("ACCOUNT_022", "1회 입금 한도를 초과했습니다", 400),

    /**
     * 출금 금액이 너무 큼
     * <p>
     * 1회 출금 한도를 초과한 경우 발생합니다.
     * </p>
     */
    WITHDRAWAL_AMOUNT_TOO_LARGE("ACCOUNT_023", "1회 출금 한도를 초과했습니다", 400),

    // ========================================
    // 상태 오류 (422 Unprocessable Entity)
    // ========================================

    /**
     * 비활성 계좌
     * <p>
     * 휴면, 동결, 해지 상태의 계좌로 거래를 시도한 경우 발생합니다.
     * </p>
     */
    ACCOUNT_NOT_ACTIVE("ACCOUNT_030", "비활성 상태의 계좌입니다", 422),

    /**
     * 이미 해지된 계좌
     * <p>
     * 이미 해지된 계좌에 대해 작업을 시도한 경우 발생합니다.
     * </p>
     */
    ACCOUNT_ALREADY_CLOSED("ACCOUNT_031", "이미 해지된 계좌입니다", 422),

    /**
     * 동결된 계좌
     * <p>
     * 동결 상태의 계좌로 거래를 시도한 경우 발생합니다.
     * 관리자에게 문의가 필요합니다.
     * </p>
     */
    ACCOUNT_FROZEN("ACCOUNT_032", "동결된 계좌입니다. 고객센터에 문의하세요", 422),

    /**
     * 휴면 계좌
     * <p>
     * 휴면 상태의 계좌로 거래를 시도한 경우 발생합니다.
     * 휴면 해제 후 이용 가능합니다.
     * </p>
     */
    ACCOUNT_DORMANT("ACCOUNT_033", "휴면 계좌입니다. 휴면 해제 후 이용하세요", 422),

    /**
     * 허용되지 않은 상태 전이
     * <p>
     * 현재 상태에서 요청한 상태로 전환이 불가능한 경우 발생합니다.
     * </p>
     */
    INVALID_STATUS_TRANSITION("ACCOUNT_034", "허용되지 않은 상태 변경입니다", 422),

    /**
     * 이미 활성 상태
     * <p>
     * 이미 활성 상태인 계좌를 활성화하려는 경우 발생합니다.
     * </p>
     */
    ACCOUNT_ALREADY_ACTIVE("ACCOUNT_035", "이미 활성 상태인 계좌입니다", 422),

    // ========================================
    // 한도 오류 (400 Bad Request)
    // ========================================

    /**
     * 일일 출금 한도 초과
     * <p>
     * 당일 출금 누적액이 일일 한도를 초과한 경우 발생합니다.
     * 다음 날 자정에 한도가 초기화됩니다.
     * </p>
     */
    DAILY_WITHDRAWAL_LIMIT_EXCEEDED("ACCOUNT_040", "일일 출금 한도를 초과했습니다", 400),

    /**
     * 일일 이체 한도 초과
     */
    DAILY_TRANSFER_LIMIT_EXCEEDED("ACCOUNT_041", "일일 이체 한도를 초과했습니다", 400),

    // ========================================
    // 동시성 오류 (409 Conflict)
    // ========================================

    /**
     * 낙관적 락 충돌
     * <p>
     * 동시에 같은 계좌를 수정하려고 할 때 발생합니다.
     * 재시도 로직으로 처리됩니다.
     * </p>
     */
    OPTIMISTIC_LOCK_CONFLICT("ACCOUNT_050", "다른 요청과 충돌이 발생했습니다. 다시 시도해주세요", 409),

    /**
     * 비관적 락 타임아웃
     * <p>
     * 락 획득 대기 시간이 초과된 경우 발생합니다.
     * </p>
     */
    PESSIMISTIC_LOCK_TIMEOUT("ACCOUNT_051", "요청 처리 시간이 초과되었습니다. 다시 시도해주세요", 409);

    private final String code;
    private final String message;
    private final int status;

    AccountErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}