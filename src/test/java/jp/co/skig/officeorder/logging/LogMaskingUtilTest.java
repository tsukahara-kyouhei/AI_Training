package jp.co.skig.officeorder.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskingUtilTest {

    /**
     * メールアドレスを正規化したうえで、ローカルパートだけを部分マスクすることを確認する。
     */
    @Test
    void maskEmail_masksAndNormalizesAddress() {
        assertThat(LogMaskingUtil.maskEmail("  Member01@Example.COM "))
                .isEqualTo("m***@example.com");
    }

    /**
     * 空値や不正なメールアドレスでは、ログ用の代替値へフォールバックすることを確認する。
     */
    @Test
    void maskEmail_returnsFallbackForBlankAndInvalidValues() {
        assertThat(LogMaskingUtil.maskEmail(" ")).isEqualTo("unknown");
        assertThat(LogMaskingUtil.maskEmail("invalid-mail")).isEqualTo("***");
    }

    /**
     * 電話番号は数字以外を除去したうえで、下4桁だけを残してマスクすることを確認する。
     */
    @Test
    void maskPhone_keepsOnlyLastFourDigits() {
        assertThat(LogMaskingUtil.maskPhone("090-1234-5678")).isEqualTo("*******5678");
        assertThat(LogMaskingUtil.maskPhone("1234")).isEqualTo("****");
    }

    /**
     * 郵便番号は先頭3桁のみを残して、それ以降をマスクすることを確認する。
     */
    @Test
    void maskPostalCode_keepsFirstThreeDigits() {
        assertThat(LogMaskingUtil.maskPostalCode("123-4567")).isEqualTo("123****");
        assertThat(LogMaskingUtil.maskPostalCode("12")).isEqualTo("***");
    }

    /**
     * セッションIDのハッシュ値が固定長の16進文字列で安定して生成されることを確認する。
     */
    @Test
    void hashSessionId_returnsDeterministicEightDigitHex() {
        String hashed = LogMaskingUtil.hashSessionId("session-001");

        assertThat(hashed).matches("^[0-9a-f]{8}$");
        assertThat(LogMaskingUtil.hashSessionId("session-001")).isEqualTo(hashed);
        assertThat(LogMaskingUtil.hashSessionId(" ")).isEqualTo("unknown");
    }
}
