package jp.co.skig.officeorder.web;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * 既にログイン済みの利用者がログイン画面へ来た場合は、購入履歴一覧へリダイレクトすることを確認する。
     */
    @Test
    void login_redirectsToMyPageWhenAlreadyAuthenticated() throws Exception {
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(
                memberSessionUser(1L, "member01@example.com", "山田", "太郎")
        ));

        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage/orders"));
    }

    /**
     * 未ログイン利用者には、正規化済みの戻り先とセッション切れ表示を含むログイン画面が返ることを確認する。
     */
    @Test
    void login_populatesLoginFormAndSessionExpiredFlagForGuest() throws Exception {
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.empty());
        when(memberSessionService.sanitizeRedirectPath(eq("/cart"))).thenReturn("/cart");
        when(assetVersionResolver.version(anyString())).thenReturn(0L);

        mockMvc.perform(get("/login")
                        .param("redirect", "/cart")
                        .param("expired", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/login"))
                .andExpect(model().attribute("sessionExpired", true))
                .andExpect(model().attribute("loginForm", Matchers.hasProperty("redirectPath", Matchers.equalTo("/cart"))));

        verify(memberSessionService).sanitizeRedirectPath("/cart");
    }
}
