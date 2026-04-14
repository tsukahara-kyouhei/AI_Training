package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

    @Mock
    private MemberService memberService;

    private MemberUserDetailsService sut;

    @BeforeEach
    void setUp() {
        sut = new MemberUserDetailsService(memberService);
    }

    // ─── loadUserByUsername ─────────────────────────────────────────────

    @Test
    void loadUserByUsername_existing_active_member_returns_principal() {
        // Arrange
        MemberCredential credential = new MemberCredential(
                1L, "test@example.com", "山田", "太郎", "$2a$10$hashed"
        );
        when(memberService.findActiveCredentialByEmail("test@example.com"))
                .thenReturn(Optional.of(credential));

        // Act
        UserDetails result = sut.loadUserByUsername("test@example.com");

        // Assert
        assertThat(result).isInstanceOf(MemberPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("$2a$10$hashed");
    }

    @Test
    void loadUserByUsername_member_not_found_throws_username_not_found_exception() {
        // Arrange
        when(memberService.findActiveCredentialByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> sut.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
