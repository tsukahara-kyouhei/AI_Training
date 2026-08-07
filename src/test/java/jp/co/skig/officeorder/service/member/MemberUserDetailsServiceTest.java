package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MemberUserDetailsServiceTest {

    private final MemberService memberService = Mockito.mock(MemberService.class);
    private final MemberUserDetailsService service = new MemberUserDetailsService(memberService);

    @Test
    void loadUserByUsername_whenCredentialExists_shouldReturnPrincipal() {
        MemberCredential credential = new MemberCredential(11L, "user@example.com", "Yamada", "Taro", "hash");
        when(memberService.findActiveCredentialByEmail(eq("user@example.com"))).thenReturn(Optional.of(credential));

        MemberPrincipal principal = (MemberPrincipal) service.loadUserByUsername("user@example.com");

        assertEquals(11L, principal.getMemberId());
        assertEquals("user@example.com", principal.getUsername());
    }

    @Test
    void loadUserByUsername_whenNotFound_shouldThrowUsernameNotFoundException() {
        when(memberService.findActiveCredentialByEmail(eq("missing@example.com"))).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
    }
}
