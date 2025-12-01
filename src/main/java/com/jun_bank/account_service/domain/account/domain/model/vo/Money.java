package com.jun_bank.account_service.domain.account.domain.model.vo;

import com.jun_bank.account_service.domain.account.domain.exception.AccountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * 금액 VO (Value Object)
 * <p>
 * 금액을 안전하게 다루기 위한 불변 객체입니다.
 * BigDecimal을 감싸서 금융 계산에 적합한 연산을 제공합니다.
 *
 * <h3>특징:</h3>
 * <ul>
 *   <li>불변 객체 (연산 결과는 새 객체 반환)</li>
 *   <li>음수 금액 허용하지 않음 (잔액 표현)</li>
 *   <li>소수점 2자리까지 (원 단위)</li>
 *   <li>HALF_UP 반올림 적용</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * Money balance = Money.of(100000);
 * Money withdrawal = Money.of(30000);
 *
 * // 잔액 확인
 * if (balance.isGreaterThanOrEqual(withdrawal)) {
 *     Money newBalance = balance.subtract(withdrawal);  // 70,000
 * }
 *
 * // 포맷팅
 * balance.formatted();  // "100,000원"
 * }</pre>
 *
 * @param amount 금액 (BigDecimal, 0 이상)
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    /**
     * 소수점 자릿수 (원 단위이므로 0)
     */
    private static final int SCALE = 0;

    /**
     * 반올림 모드
     */
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 0원
     */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /**
     * Money 생성자 (Compact Constructor)
     * <p>
     * 음수 금액은 허용하지 않습니다.
     * </p>
     *
     * @param amount 금액
     * @throws AccountException 금액이 null이거나 음수인 경우 (ACCOUNT_004)
     */
    public Money {
        if (amount == null) {
            throw AccountException.invalidAmount(null);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw AccountException.invalidAmount(amount);
        }
        // 스케일 정규화
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * long 값으로 Money 생성
     *
     * @param amount 금액
     * @return Money 객체
     */
    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    /**
     * BigDecimal로 Money 생성
     *
     * @param amount 금액
     * @return Money 객체
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * 문자열로 Money 생성
     *
     * @param amount 금액 문자열
     * @return Money 객체
     */
    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    /**
     * 0원 여부 확인
     *
     * @return 0원이면 true
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 양수(0보다 큰) 여부 확인
     *
     * @return 양수이면 true
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 다른 금액보다 큰지 확인
     *
     * @param other 비교 대상
     * @return 크면 true
     */
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 다른 금액보다 크거나 같은지 확인
     *
     * @param other 비교 대상
     * @return 크거나 같으면 true
     */
    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * 다른 금액보다 작은지 확인
     *
     * @param other 비교 대상
     * @return 작으면 true
     */
    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * 다른 금액보다 작거나 같은지 확인
     *
     * @param other 비교 대상
     * @return 작거나 같으면 true
     */
    public boolean isLessThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) <= 0;
    }

    /**
     * 금액 더하기
     *
     * @param other 더할 금액
     * @return 새로운 Money 객체 (원본 불변)
     */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /**
     * 금액 빼기
     * <p>
     * 결과가 음수가 되면 예외가 발생합니다.
     * 빼기 전에 {@link #isGreaterThanOrEqual(Money)}로 확인하세요.
     * </p>
     *
     * @param other 뺄 금액
     * @return 새로운 Money 객체 (원본 불변)
     * @throws AccountException 결과가 음수인 경우
     */
    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw AccountException.insufficientBalance(this.amount, other.amount);
        }
        return new Money(result);
    }

    /**
     * 금액 곱하기 (이자 계산 등)
     *
     * @param multiplier 승수
     * @return 새로운 Money 객체
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier).setScale(SCALE, ROUNDING_MODE));
    }

    /**
     * 한국 원화 형식으로 포맷팅
     *
     * @return 포맷된 문자열 (예: "100,000원")
     */
    public String formatted() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(amount) + "원";
    }

    /**
     * 숫자만 반환 (쉼표 포함)
     *
     * @return 포맷된 숫자 (예: "100,000")
     */
    public String formattedNumber() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(amount);
    }

    /**
     * long 값으로 변환
     *
     * @return long 값
     */
    public long toLong() {
        return amount.longValue();
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}