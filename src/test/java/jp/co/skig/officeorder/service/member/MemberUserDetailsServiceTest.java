package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link MemberUserDetailsService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

    @InjectMocks
    MemberUserDetailsService sut;

    @Mock
    MemberService memberService;

    // ── loadUserByUsername ───────────────────────────────────────────────

    @Test
    void 有効会員のメールアドレスを渡すとMemberPrincipalが返ること() {
        var credential = new MemberCredential(1L, "test@example.com", "山田", "太郎", "$2a$10$hash");
        when(memberService.findActiveCredentialByEmail("test@example.com"))
                .thenReturn(Optional.of(credential));

        var result = sut.loadUserByUsername("test@example.com");

        assertThat(result).isInstanceOf(MemberPrincipal.class);
        var principal = (MemberPrincipal) result;
        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("test@example.com");
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hash");
    }

    @Test
    void 存在しないメールアドレスを渡すとUsernameNotFoundExceptionをスローすること() {
        when(memberService.findActiveCredentialByEmail("noexist@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("noexist@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
