package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberUserDetailsServiceTest {

    private MemberService memberService;
    private MemberUserDetailsService sut;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        sut = new MemberUserDetailsService(memberService);
    }

    // --- loadUserByUsername ---

    @Test
    @DisplayName("メールで会員が見つかった場合は MemberPrincipal を返す")
    void loadUserByUsername_found_returnsMemberPrincipal() {
        MemberCredential credential = new MemberCredential(1L, "test@example.com", "山田", "太郎", "hash");
        when(memberService.findActiveCredentialByEmail("test@example.com")).thenReturn(Optional.of(credential));

        UserDetails result = sut.loadUserByUsername("test@example.com");

        assertThat(result).isInstanceOf(MemberPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("hash");
        MemberPrincipal principal = (MemberPrincipal) result;
        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getLastName()).isEqualTo("山田");
        assertThat(principal.getFirstName()).isEqualTo("太郎");
    }

    @Test
    @DisplayName("会員が見つからない場合は UsernameNotFoundException をスローする")
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(memberService.findActiveCredentialByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("空メールアドレスの場合は UsernameNotFoundException をスローする（会員なし）")
    void loadUserByUsername_blankEmail_throwsUsernameNotFoundException() {
        when(memberService.findActiveCredentialByEmail("   ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
