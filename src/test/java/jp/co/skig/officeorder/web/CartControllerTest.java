package jp.co.skig.officeorder.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.order.OrderService;
import jp.co.skig.officeorder.web.auth.LoginEmailCookieService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private MemberSessionService memberSessionService;

    @MockBean
    private LoginEmailCookieService loginEmailCookieService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private OrderService orderService;

    @MockBean
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

    @Test
    void applyCoupon_正常系_クーポンを適用してカートへリダイレクトする() throws Exception {

        CartView cart = mock(CartView.class);

        when(cartService.getCart(any(), any()))
                .thenReturn(cart);

        mockMvc.perform(post("/cart/coupon/apply")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("couponCode", "SAVE1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute(
                        "couponSuccess",
                        "クーポンを適用しました。"));

        verify(cartService).getCart(any(), any());

        verify(cartService).applyCoupon(
                eq("SAVE1000"),
                eq(cart),
                any(),
                any());
    }

    @Test
    void applyCoupon_異常系_クーポン適用に失敗した場合はエラーメッセージを表示する() throws Exception {

        CartView cart = mock(CartView.class);

        when(cartService.getCart(any(), any()))
                .thenReturn(cart);

        doThrow(new IllegalArgumentException("無効なクーポンコード、または期限切れです。"))
                .when(cartService)
                .applyCoupon(
                        anyString(),
                        eq(cart),
                        any(),
                        any());

        mockMvc.perform(post("/cart/coupon/apply")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("couponCode", "INVALID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute(
                        "couponError",
                        "無効なクーポンコード、または期限切れです。"));
    }

    @Test
    void removeCoupon_正常系_クーポンを解除してカートへリダイレクトする() throws Exception {

        mockMvc.perform(post("/cart/coupon/remove")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute(
                        "couponSuccess",
                        "クーポンを解除しました。"));
    }
}
