package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

    @Mock
    MemberService memberService;

    @InjectMocks
    MemberUserDetailsService memberUserDetailsService;

    // --- loadUserByUsername ---

    @Test
    void loadUserByUsername_existingMember_returnsMemberPrincipal() {
        MemberCredential credential = new MemberCredential(1L, "taro@example.com", "山田", "太郎", "$2a$hash");
        when(memberService.findActiveCredentialByEmail("taro@example.com"))
                .thenReturn(Optional.of(credential));

        UserDetails result = memberUserDetailsService.loadUserByUsername("taro@example.com");

        assertThat(result).isInstanceOf(MemberPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("taro@example.com");
        assertThat(result.getPassword()).isEqualTo("$2a$hash");
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(memberService.findActiveCredentialByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberUserDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
