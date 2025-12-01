package com.jun_bank.account_service.domain.account.domain.model.vo;

import com.jun_bank.account_service.domain.account.domain.exception.AccountException;
import com.jun_bank.account_service.domain.account.domain.model.AccountType;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 계좌번호 VO (Value Object)
 * <p>
 * 사용자에게 노출되는 계좌번호입니다.
 * Luhn 알고리즘으로 체크섬을 검증합니다.
 *
 * <h3>계좌번호 형식:</h3>
 * <pre>XXX-XXXX-XXXX-XX (14자리)</pre>
 * <ul>
 *   <li>앞 3자리: 계좌 유형 프리픽스 (110: 입출금, 220: 저축, 330: 정기예금)</li>
 *   <li>중간 8자리: 랜덤 숫자</li>
 *   <li>마지막 2자리: 체크섬 (Luhn 알고리즘)</li>
 * </ul>
 *
 * <h3>Luhn 알고리즘:</h3>
 * <p>
 * 신용카드, 계좌번호 등의 유효성 검증에 사용되는 체크섬 알고리즘입니다.
 * 단순 입력 오류를 감지할 수 있습니다.
 * </p>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 새 계좌번호 생성
 * String number = AccountNumber.generate(AccountType.CHECKING);
 * // 결과: "110-1234-5678-90"
 *
 * // 기존 계좌번호 검증
 * AccountNumber accountNumber = AccountNumber.of("110-1234-5678-90");
 *
 * // 마스킹
 * accountNumber.masked();  // "110-****-****-90"
 * }</pre>
 *
 * @param value 계좌번호 문자열 (XXX-XXXX-XXXX-XX 형식)
 */
public record AccountNumber(String value) {

    /**
     * 계좌번호 패턴 (XXX-XXXX-XXXX-XX)
     */
    private static final Pattern ACCOUNT_NUMBER_PATTERN =
            Pattern.compile("^\\d{3}-\\d{4}-\\d{4}-\\d{2}$");

    /**
     * 난수 생성기
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * AccountNumber 생성자 (Compact Constructor)
     * <p>
     * 형식 검증과 Luhn 체크섬 검증을 수행합니다.
     * </p>
     *
     * @param value 계좌번호 문자열
     * @throws AccountException 형식이 잘못되었거나 체크섬 검증 실패 시 (ACCOUNT_002)
     */
    public AccountNumber {
        if (value == null || !ACCOUNT_NUMBER_PATTERN.matcher(value).matches()) {
            throw AccountException.invalidAccountNumberFormat(value);
        }
        if (!validateLuhn(value)) {
            throw AccountException.invalidAccountNumberFormat(value);
        }
    }

    /**
     * 문자열로부터 AccountNumber 객체 생성
     *
     * @param value 계좌번호 문자열
     * @return AccountNumber 객체
     * @throws AccountException 유효하지 않은 계좌번호인 경우
     */
    public static AccountNumber of(String value) {
        return new AccountNumber(value);
    }

    /**
     * 새로운 계좌번호 생성
     * <p>
     * 계좌 유형에 따른 프리픽스 + 랜덤 숫자 + Luhn 체크섬으로 구성됩니다.
     * </p>
     *
     * @param accountType 계좌 유형
     * @return 생성된 계좌번호 문자열 (XXX-XXXX-XXXX-XX 형식)
     */
    public static String generate(AccountType accountType) {
        String prefix = accountType.getAccountNumberPrefix();  // "110", "220", "330"

        // 중간 8자리 랜덤 생성
        StringBuilder middle = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            middle.append(RANDOM.nextInt(10));
        }

        // 체크섬 계산을 위한 11자리
        String withoutChecksum = prefix + middle.toString();

        // Luhn 체크섬 2자리 계산
        String checksum = calculateLuhnChecksum(withoutChecksum);

        // 포맷팅: XXX-XXXX-XXXX-XX
        String full = withoutChecksum + checksum;
        return formatAccountNumber(full);
    }

    /**
     * 마스킹된 계좌번호 반환
     * <p>
     * 개인정보 보호를 위해 중간 부분을 마스킹합니다.
     * </p>
     *
     * @return 마스킹된 계좌번호 (예: "110-****-****-90")
     */
    public String masked() {
        String[] parts = value.split("-");
        return parts[0] + "-****-****-" + parts[3];
    }

    /**
     * 하이픈 없는 계좌번호 반환
     *
     * @return 숫자만 있는 계좌번호 (예: "11012345678")
     */
    public String withoutHyphen() {
        return value.replace("-", "");
    }

    /**
     * 계좌 유형 프리픽스 반환
     *
     * @return 앞 3자리 (예: "110")
     */
    public String getPrefix() {
        return value.substring(0, 3);
    }

    // ========================================
    // Private 유틸리티 메서드
    // ========================================

    /**
     * Luhn 체크섬 검증
     *
     * @param accountNumber 검증할 계좌번호
     * @return 유효하면 true
     */
    private static boolean validateLuhn(String accountNumber) {
        String digits = accountNumber.replace("-", "");
        int sum = 0;
        boolean alternate = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(digits.charAt(i));

            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }

            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Luhn 체크섬 2자리 계산
     *
     * @param baseNumber 체크섬을 제외한 숫자 (11자리)
     * @return 2자리 체크섬 문자열
     */
    private static String calculateLuhnChecksum(String baseNumber) {
        // 첫 번째 체크 디짓 계산 (12번째 자리)
        int checkDigit1 = calculateSingleLuhnDigit(baseNumber);
        String withFirstCheck = baseNumber + checkDigit1;

        // 두 번째 체크 디짓 계산 (13번째 자리)
        int checkDigit2 = calculateSingleLuhnDigit(withFirstCheck);

        return String.valueOf(checkDigit1) + checkDigit2;
    }

    /**
     * 단일 Luhn 체크 디짓 계산
     */
    private static int calculateSingleLuhnDigit(String number) {
        int sum = 0;
        boolean alternate = true;  // 마지막부터 시작하므로 true

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }

            sum += n;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    /**
     * 14자리 숫자를 XXX-XXXX-XXXX-XX 형식으로 변환
     */
    private static String formatAccountNumber(String digits) {
        return digits.substring(0, 3) + "-" +
                digits.substring(3, 7) + "-" +
                digits.substring(7, 11) + "-" +
                digits.substring(11, 13);
    }
}