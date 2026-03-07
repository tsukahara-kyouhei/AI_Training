package jp.co.skig.officeorder.web;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.order.OrderService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static jp.co.skig.officeorder.testutil.CartTestFixtures.cartView;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberAdditionalAddressPage;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * カート画面では、取得したカートモデルと空判定結果をそのまま表示モデルへ積むことを確認する。
     */
    @Test
    void cart_rendersCartViewAndEmptyFlag() throws Exception {
        var cart = cartView();
        when(cartService.getCart(any(), any())).thenReturn(cart);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/cart"))
                .andExpect(model().attribute("cart", cart))
                .andExpect(model().attribute("isCartEmpty", false));
    }

    /**
     * 注文情報入力画面では、ログイン会員の追加お届け先を選択肢として表示することを確認する。
     */
    @Test
    void checkoutInput_populatesAdditionalAddressOptionsForLoggedInMember() throws Exception {
        var cart = cartView();
        CheckoutInputForm initialForm = new CheckoutInputForm();
        var member = memberSessionUser();
        when(cartService.getCart(any(), any())).thenReturn(cart);
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(member));
        when(orderService.createInitialForm(Optional.of(member))).thenReturn(initialForm);
        when(memberService.findAdditionalAddresses(5L, 1)).thenReturn(memberAdditionalAddressPage(5L));

        mockMvc.perform(get("/checkout/input"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/checkout-input"))
                .andExpect(model().attribute("hasCheckoutAdditionalAddresses", true))
                .andExpect(model().attribute("checkoutAdditionalAddresses", Matchers.hasSize(1)));
    }

    /**
     * 注文情報入力で法人必須の会社名が未入力なら、確認画面へ進まず入力画面にエラー付きで戻ることを確認する。
     */
    @Test
    void checkoutInputSubmit_returnsInputPageWhenCorporateCompanyNameIsMissing() throws Exception {
        when(cartService.getCart(any(), any())).thenReturn(cartView());
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/checkout/input")
                        .param("personalOrCorporate", "corporate")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("lastNameKana", "ヤマダ")
                        .param("firstNameKana", "ハナコ")
                        .param("companyName", "")
                        .param("departmentName", "営業部")
                        .param("email", "member@example.com")
                        .param("daytimePhone", "0312345678")
                        .param("fax", "0312345679")
                        .param("postalCodePart1", "100")
                        .param("postalCodePart2", "0001")
                        .param("prefecture", "東京都")
                        .param("city", "千代田区")
                        .param("addressLine", "1-1-1")
                        .param("deliveryFloor", "5")
                        .param("hasElevator", "true")
                        .param("paymentMethod", "bank_transfer"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/checkout-input"))
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "companyName"));
    }

    /**
     * 登録済みお届け先選択に変換不能な値が入力された場合は、内部の型変換エラー文言ではなく利用者向けメッセージを表示することを確認する。
     */
    @Test
    void checkoutInputSubmit_showsFriendlyMessageWhenAdditionalAddressSelectionIsInvalid() throws Exception {
        when(cartService.getCart(any(), any())).thenReturn(cartView());
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(memberSessionUser()));
        when(memberService.findAdditionalAddresses(5L, 1)).thenReturn(memberAdditionalAddressPage(5L));

        mockMvc.perform(post("/checkout/input")
                        .param("personalOrCorporate", "personal")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("lastNameKana", "ヤマダ")
                        .param("firstNameKana", "ハナコ")
                        .param("companyName", "")
                        .param("departmentName", "営業部")
                        .param("email", "member@example.com")
                        .param("daytimePhone", "0312345678")
                        .param("fax", "0312345679")
                        .param("postalCodePart1", "100")
                        .param("postalCodePart2", "0001")
                        .param("prefecture", "東京都")
                        .param("city", "千代田区")
                        .param("addressLine", "1-1-1")
                        .param("deliveryFloor", "5")
                        .param("hasElevator", "true")
                        .param("paymentMethod", "bank_transfer")
                        .param("selectedAdditionalAddressId", "unexpected"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/checkout-input"))
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "selectedAdditionalAddressId"))
                .andExpect(content().string(Matchers.containsString("登録済みお届け先の選択値が不正です。")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Failed to convert property value"))));
    }

    /**
     * 注文確認確定時にワンタイムトークン不一致が起きた場合は、購入手続きを破棄して入力画面へ戻すことを確認する。
     */
    @Test
    void placeOrder_redirectsToInputWhenTokenDoesNotMatchSession() throws Exception {
        var cart = cartView();
        CheckoutInputForm sessionForm = new CheckoutInputForm();
        when(cartService.getCart(any(), any())).thenReturn(cart);

        mockMvc.perform(post("/checkout/confirm")
                        .sessionAttr("checkout_input_form", sessionForm)
                        .sessionAttr("checkout_confirm_token", "expected-token")
                        .param("token", "different-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/input"))
                .andExpect(flash().attribute("checkoutError", "注文の再送信を検出しました。入力内容を再確認してください。"));

        verify(orderService, never()).placeOrder(any(), any(), any());
    }
}
