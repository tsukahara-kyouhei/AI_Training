package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class MemberUserDetailsServiceTest {

    @Test
    void loadUserByUsername_正常系_会員が存在すればprincipalを返す() {
        MemberService memberService = mock(MemberService.class);
        MemberUserDetailsService service = new MemberUserDetailsService(memberService);
        MemberCredential credential = new MemberCredential(1L, "user@example.com", "山田", "太郎", "hash");
        when(memberService.findActiveCredentialByEmail("user@example.com")).thenReturn(Optional.of(credential));

        UserDetails result = service.loadUserByUsername("user@example.com");

        assertThat(result.getUsername()).isEqualTo("user@example.com");
        assertThat(result.getPassword()).isEqualTo("hash");
    }

    @Test
    void loadUserByUsername_異常系_会員が存在しなければ例外を送出する() {
        MemberService memberService = mock(MemberService.class);
        MemberUserDetailsService service = new MemberUserDetailsService(memberService);
        when(memberService.findActiveCredentialByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
    }
}
