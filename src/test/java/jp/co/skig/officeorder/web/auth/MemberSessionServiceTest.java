package jp.co.skig.officeorder.web.auth;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;

class MemberSessionServiceTest {

    private final MemberSessionService service = new MemberSessionService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 匿名認証状態では、現在会員を取得できず空の結果になることを確認する。
     */
    @Test
    void currentMember_returnsEmptyWhenAuthenticationIsAnonymous() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<MemberSessionUser> actual = service.currentMember(mock(HttpSession.class));

        assertThat(actual).isEmpty();
    }

    /**
     * 会員 principal が認証済みで入っている場合は、画面表示用の会員情報へ変換して返すことを確認する。
     */
    @Test
    void currentMember_returnsMemberSessionUserForAuthenticatedMember() {
        MemberPrincipal principal = new MemberPrincipal(10L, "member@example.com", "山田", "花子", "hashed");
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<MemberSessionUser> actual = service.currentMember(mock(HttpSession.class));

        assertThat(actual).contains(memberSessionUser(10L, "member@example.com", "山田", "花子"));
    }

    /**
     * login 実行時に SecurityContext とセッション属性へ認証情報が保存され、その直後から現在会員取得できることを確認する。
     */
    @Test
    void login_storesSecurityContextInSessionAndAuthenticatesMember() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MemberSessionUser member = memberSessionUser(5L, "member@example.com", "佐藤", "一郎");

        service.login(request, member);

        assertThat(service.currentMember(request.getSession())).contains(member);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(request.getSession().getAttribute("SPRING_SECURITY_CONTEXT"))
                .isSameAs(SecurityContextHolder.getContext());
    }

    /**
     * clear 実行時に SecurityContext が破棄され、既存セッションが invalidate されることを確認する。
     */
    @Test
    void clear_invalidatesSessionAndClearsSecurityContext() {
        MemberPrincipal principal = new MemberPrincipal(1L, "member@example.com", "田中", "次郎", "hashed");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
        );
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session-1");

        service.clear(session);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(session).invalidate();
    }
}
