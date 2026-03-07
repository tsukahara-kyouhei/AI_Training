package jp.co.skig.officeorder.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberOrderDetailView;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;

@WebMvcTest(controllers = MyPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * マイページトップは既定導線として購入履歴一覧へリダイレクトすることを確認する。
     */
    @Test
    void mypageTop_redirectsToOrders() throws Exception {
        mockMvc.perform(get("/mypage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage/orders"));
    }

    /**
     * 購入履歴一覧で存在しないページが指定された場合は、最終ページで再取得した結果を表示することを確認する。
     */
    @Test
    void orders_refetchesLastPageWhenRequestedPageIsOutOfRange() throws Exception {
        var member = memberSessionUser();
        MemberOrderHistoryPage first = new MemberOrderHistoryPage(List.of(), 12L, 8, 10);
        MemberOrderHistoryPage corrected = new MemberOrderHistoryPage(
                List.of(new MemberOrderHistoryView(
                        "20260307000001",
                        OffsetDateTime.parse("2026-03-07T10:00:00+09:00"),
                        BigDecimal.valueOf(12000),
                        "completed"
                )),
                12L,
                2,
                10
        );
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(member));
        when(orderService.findMemberOrderHistories(5L, 8)).thenReturn(first);
        when(orderService.findMemberOrderHistories(5L, 2)).thenReturn(corrected);

        mockMvc.perform(get("/mypage/orders").param("page", "8"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/mypage-orders-list"))
                .andExpect(model().attribute("orders", corrected.items()))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 2));
    }

    /**
     * 再購入で一部商品だけ追加失敗した場合は、成功メッセージと失敗商品コードの警告を両方返すことを確認する。
     */
    @Test
    void reorder_setsSuccessAndPartialFailureMessages() throws Exception {
        var member = memberSessionUser();
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(member));
        when(orderService.findMemberOrderDetail(5L, "20260307000001")).thenReturn(Optional.of(memberOrderDetailView()));
        when(orderService.findReorderItems(5L, "20260307000001")).thenReturn(List.of(
                new OrderReorderItem(11L, "P0001-C01", 1, true),
                new OrderReorderItem(12L, "P0002-C02", 2, false)
        ));
        doThrow(new IllegalArgumentException("在庫不足"))
                .when(cartService)
                .addItem(any(), any(), org.mockito.ArgumentMatchers.eq(12L), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq(false));

        mockMvc.perform(post("/mypage/orders/20260307000001/reorder"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartMessage", "再購入商品をカートに追加しました。"))
                .andExpect(flash().attribute("cartError", Matchers.containsString("P0002-C02")));
    }

    /**
     * 未ログインでマイページ配下にアクセスした場合は、認可エラーとして 401 を返すことを確認する。
     */
    @Test
    void orders_returnsUnauthorizedWhenMemberIsNotLoggedIn() throws Exception {
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/mypage/orders"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 会員情報変更で生年月日/記念日に変換不能な値が入力された場合は、内部例外文言ではなく業務向けメッセージを表示することを確認する。
     */
    @Test
    void updateProfile_showsFriendlyMessageWhenAnniversaryDateIsInvalid() throws Exception {
        var member = memberSessionUser();
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(member));

        mockMvc.perform(post("/mypage/profile")
                        .param("personalOrCorporate", "personal")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("lastNameKana", "ヤマダ")
                        .param("firstNameKana", "ハナコ")
                        .param("companyName", "")
                        .param("departmentName", "営業部")
                        .param("email", "member@example.com")
                        .param("gender", "female")
                        .param("anniversaryDate", "11985-06-09")
                        .param("newsletterOptIn", "true")
                        .param("postalCodePart1", "100")
                        .param("postalCodePart2", "0001")
                        .param("prefecture", "東京都")
                        .param("city", "千代田区")
                        .param("addressLine", "1-1-1")
                        .param("deliveryFloor", "5")
                        .param("hasElevator", "true")
                        .param("daytimePhone", "0312345678")
                        .param("fax", "0312345679"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/mypage-profile-edit"))
                .andExpect(model().attributeHasFieldErrors("profileForm", "anniversaryDate"))
                .andExpect(content().string(Matchers.containsString("生年月日/記念日は正しい日付を入力してください。")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Failed to convert property value"))));
    }

    /**
     * 会員情報変更でエレベーター有無に変換不能な値が入力された場合は、内部例外文言ではなく利用者向けメッセージを表示することを確認する。
     */
    @Test
    void updateProfile_showsFriendlyMessageWhenHasElevatorIsInvalid() throws Exception {
        var member = memberSessionUser();
        when(memberSessionService.currentMember(any(HttpSession.class))).thenReturn(Optional.of(member));

        mockMvc.perform(post("/mypage/profile")
                        .param("personalOrCorporate", "personal")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("lastNameKana", "ヤマダ")
                        .param("firstNameKana", "ハナコ")
                        .param("companyName", "")
                        .param("departmentName", "営業部")
                        .param("email", "member@example.com")
                        .param("gender", "female")
                        .param("anniversaryDate", "1985-06-09")
                        .param("newsletterOptIn", "true")
                        .param("postalCodePart1", "100")
                        .param("postalCodePart2", "0001")
                        .param("prefecture", "東京都")
                        .param("city", "千代田区")
                        .param("addressLine", "1-1-1")
                        .param("deliveryFloor", "5")
                        .param("hasElevator", "unexpected")
                        .param("daytimePhone", "0312345678")
                        .param("fax", "0312345679"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/mypage-profile-edit"))
                .andExpect(model().attributeHasFieldErrors("profileForm", "hasElevator"))
                .andExpect(content().string(Matchers.containsString("エレベーターの有無を選択してください。")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Failed to convert property value"))));
    }
}
