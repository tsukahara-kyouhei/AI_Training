package jp.co.skig.officeorder.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.order.OrderService;
import jp.co.skig.officeorder.web.auth.LoginEmailCookieService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private LoginEmailCookieService loginEmailCookieService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AnnouncementService announcementService;

    @Test
    void updateItemTreatsCheckedAssemblyOptionAsTrueWhenHiddenAndCheckboxValuesAreBothSent() throws Exception {
        mockMvc.perform(post("/cart/items/42/update")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("quantity", "2")
                        .param("assemblyRequested", "false")
                        .param("assemblyRequested", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).updateItem(any(), any(), eq(42L), eq(2), eq(true));
    }
}
