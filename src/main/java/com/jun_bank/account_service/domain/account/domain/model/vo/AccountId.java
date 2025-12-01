package com.jun_bank.account_service.domain.account.domain.model.vo;

import com.jun_bank.account_service.domain.account.domain.exception.AccountException;
import com.jun_bank.common_lib.util.UuidUtils;

/**
 * 계좌 식별자 VO (Value Object)
 * <p>
 * 계좌의 고유 식별자입니다.
 * 계좌번호(AccountNumber)와는 별개로 시스템 내부에서 사용됩니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>ACC-xxxxxxxx (예: ACC-a1b2c3d4)</pre>
 * <ul>
 *   <li>ACC: 계좌 도메인 프리픽스 (고정)</li>
 *   <li>-: 구분자</li>
 *   <li>xxxxxxxx: 8자리 랜덤 영숫자 (UUID 기반)</li>
 * </ul>
 *
 * <h3>AccountNumber와의 차이:</h3>
 * <ul>
 *   <li>AccountId: 내부 시스템 식별자 (ACC-xxx)</li>
 *   <li>AccountNumber: 사용자에게 노출되는 계좌번호 (110-1234-5678-90)</li>
 * </ul>
 *
 * @param value 계좌 ID 문자열 (ACC-xxxxxxxx 형식)
 * @see AccountNumber
 */
public record AccountId(String value) {

    /**
     * ID 프리픽스
     */
    public static final String PREFIX = "ACC";

    /**
     * AccountId 생성자 (Compact Constructor)
     *
     * @param value 계좌 ID 문자열
     * @throws AccountException ID 형식이 유효하지 않은 경우 (ACCOUNT_001)
     */
    public AccountId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw AccountException.invalidAccountIdFormat(value);
        }
    }

    /**
     * 문자열로부터 AccountId 객체 생성
     *
     * @param value 계좌 ID 문자열
     * @return AccountId 객체
     * @throws AccountException ID 형식이 유효하지 않은 경우
     */
    public static AccountId of(String value) {
        return new AccountId(value);
    }

    /**
     * 새로운 계좌 ID 생성
     * <p>
     * Entity 레이어에서 새 계좌를 저장할 때 호출합니다.
     * </p>
     *
     * @return 생성된 ID 문자열 (ACC-xxxxxxxx 형식)
     */
    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}