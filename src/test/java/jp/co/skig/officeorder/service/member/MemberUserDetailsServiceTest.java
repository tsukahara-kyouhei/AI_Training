package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

    @Mock
    private MemberService memberService;

    private MemberUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new MemberUserDetailsService(memberService);
    }

    @Test
    @DisplayName("存在するメールアドレスで検索した場合、正しいUserDetails(MemberPrincipal)が返されること")
    void loadUserByUsername_whenUserExists_returnsUserDetails() {
        // 準備
        String email = "test@example.com";
        MemberCredential credential = new MemberCredential(
                100L,
                email,
                "山田",
                "太郎",
                "$2a$10$hashedpassword..."
        );
        when(memberService.findActiveCredentialByEmail(email)).thenReturn(Optional.of(credential));

        // 実行
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // 検証
        assertThat(userDetails).isNotNull().isInstanceOf(MemberPrincipal.class);
        MemberPrincipal principal = (MemberPrincipal) userDetails;
        assertThat(principal.getUsername()).isEqualTo(email);
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hashedpassword...");
        
        verify(memberService).findActiveCredentialByEmail(email);
    }

    @Test
    @DisplayName("存在しないメールアドレスで検索した場合、UsernameNotFoundExceptionがスローされること")
    void loadUserByUsername_whenUserNotFound_throwsUsernameNotFoundException() {
        // 準備
        String email = "notfound@example.com";
        when(memberService.findActiveCredentialByEmail(email)).thenReturn(Optional.empty());

        // 実行 兼 検証
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("会員が見つかりません。");

        verify(memberService).findActiveCredentialByEmail(email);
    }
}