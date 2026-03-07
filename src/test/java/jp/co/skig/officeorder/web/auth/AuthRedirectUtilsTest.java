package jp.co.skig.officeorder.web.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRedirectUtilsTest {

    /**
     * アプリ内相対パスは、前後の空白を除去したうえで戻り先として採用されることを確認する。
     */
    @Test
    void sanitizeRedirectPath_returnsRelativeApplicationPath() {
        assertThat(AuthRedirectUtils.sanitizeRedirectPath(" /products/10?color=2 "))
                .isEqualTo("/products/10?color=2");
    }

    /**
     * 空値、外部URL、認証系URLなどの危険な戻り先は拒否されることを確認する。
     */
    @Test
    void sanitizeRedirectPath_rejectsUnsafeOrDisallowedPaths() {
        assertThat(AuthRedirectUtils.sanitizeRedirectPath(null)).isNull();
        assertThat(AuthRedirectUtils.sanitizeRedirectPath("")).isNull();
        assertThat(AuthRedirectUtils.sanitizeRedirectPath("products/10")).isNull();
        assertThat(AuthRedirectUtils.sanitizeRedirectPath("//example.com")).isNull();
        assertThat(AuthRedirectUtils.sanitizeRedirectPath("https://example.com")).isNull();
        assertThat(AuthRedirectUtils.sanitizeRedirectPath("/login")).isNull();
        assertThat(AuthRedirectUtils.sanitizeRedirectPath("/logout")).isNull();
    }
}
