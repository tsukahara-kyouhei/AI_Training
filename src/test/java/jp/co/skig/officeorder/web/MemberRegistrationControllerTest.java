package jp.co.skig.officeorder.web;

import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.web.auth.AuthSessionKeys;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberRegisterForm;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;

@WebMvcTest(controllers = MemberRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private NotificationMailService notificationMailService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * 登録入力画面では、セッションに確認待ちフォームが残っている場合にその内容を初期表示へ再利用することを確認する。
     */
    @Test
    void showForm_prefersPendingFormStoredInSession() throws Exception {
        MemberRegisterForm pendingForm = memberRegisterForm();
        pendingForm.setEmail("pending@example.com");

        mockMvc.perform(get("/members/register")
                        .sessionAttr(AuthSessionKeys.PENDING_REGISTER_FORM, pendingForm))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/member-register"))
                .andExpect(model().attribute("registerForm",
                        Matchers.hasProperty("email", Matchers.equalTo("pending@example.com"))));
    }

    /**
     * 確認画面遷移時に法人必須の会社名が未入力なら、入力画面へ戻って companyName のエラーが表示されることを確認する。
     */
    @Test
    void confirm_returnsInputPageWhenCorporateCompanyNameIsMissing() throws Exception {
        when(memberService.existsByEmail("member@example.com")).thenReturn(false);

        mockMvc.perform(post("/members/register/confirm")
                        .param("personalOrCorporate", "corporate")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("lastNameKana", "ヤマダ")
                        .param("firstNameKana", "ハナコ")
                        .param("companyName", "")
                        .param("departmentName", "営業部")
                        .param("email", "member@example.com")
                        .param("gender", "female")
                        .param("anniversaryDate", "1990-01-01")
                        .param("password", "Password!1")
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
                .andExpect(view().name("pages/member-register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "companyName"));
    }

    /**
     * 生年月日/記念日に変換不能な値が入力された場合でも、内部の型変換エラー文言ではなく業務向けのエラーメッセージを表示することを確認する。
     */
    @Test
    void confirm_showsFriendlyMessageWhenAnniversaryDateIsInvalid() throws Exception {
        when(memberService.existsByEmail("member@example.com")).thenReturn(false);

        mockMvc.perform(post("/members/register/confirm")
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
                        .param("password", "Password!1")
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
                .andExpect(view().name("pages/member-register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "anniversaryDate"))
                .andExpect(content().string(Matchers.containsString("生年月日/記念日は正しい日付を入力してください。")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Failed to convert property value"))));
    }

    /**
     * メールマガジン希望に変換不能な値が入力された場合は、内部の型変換エラー文言ではなく利用者向けメッセージを表示することを確認する。
     */
    @Test
    void confirm_showsFriendlyMessageWhenNewsletterOptInIsInvalid() throws Exception {
        when(memberService.existsByEmail("member@example.com")).thenReturn(false);

        mockMvc.perform(post("/members/register/confirm")
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
                        .param("password", "Password!1")
                        .param("newsletterOptIn", "unexpected")
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
                .andExpect(view().name("pages/member-register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "newsletterOptIn"))
                .andExpect(content().string(Matchers.containsString("メールマガジンの送付希望を選択してください。")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Failed to convert property value"))));
    }

    /**
     * 入力内容が妥当な場合は、正規化済みフォームをセッションへ保存して確認画面を表示することを確認する。
     */
    @Test
    void confirm_storesNormalizedFormInSessionAndShowsConfirmPage() throws Exception {
        when(memberService.existsByEmail("member@example.com")).thenReturn(false);

        mockMvc.perform(post("/members/register/confirm")
                        .param("personalOrCorporate", "personal")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("lastNameKana", "ヤマダ")
                        .param("firstNameKana", "ハナコ")
                        .param("companyName", "")
                        .param("departmentName", "営業部")
                        .param("email", "member@example.com")
                        .param("gender", "female")
                        .param("anniversaryDate", "1990-01-01")
                        .param("password", "Password!1")
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
                .andExpect(view().name("pages/member-register-confirm"))
                .andExpect(model().attribute("registerForm",
                        Matchers.hasProperty("email", Matchers.equalTo("member@example.com"))))
                .andExpect(request().sessionAttribute(AuthSessionKeys.PENDING_REGISTER_FORM,
                        Matchers.hasProperty("email", Matchers.equalTo("member@example.com"))));
    }

    /**
     * 会員登録確定時は、登録完了後に自動ログインと完了メール送信を行い、完了画面へリダイレクトすることを確認する。
     */
    @Test
    void register_createsMemberLogsInAndRedirectsToComplete() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MemberRegisterForm pendingForm = memberRegisterForm();
        var created = memberSessionUser(7L, "member@example.com", "山田", "花子");
        session.setAttribute(AuthSessionKeys.PENDING_REGISTER_FORM, pendingForm);
        when(memberService.register(any(MemberRegisterForm.class))).thenReturn(created);

        mockMvc.perform(post("/members/register").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/register/complete"))
                .andExpect(flash().attribute("registeredMemberCode", "MEM0000007"));

        verify(memberService).register(any(MemberRegisterForm.class));
        verify(memberSessionService).login(any(), eq(created));
        verify(notificationMailService).sendMemberRegistrationCompleteMail(created);
    }

    /**
     * 登録確定に必要な確認待ちフォームがセッションにない場合は、入力画面へ戻すことを確認する。
     */
    @Test
    void register_redirectsToInputWhenPendingFormIsMissing() throws Exception {
        mockMvc.perform(post("/members/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/register"));

        verify(memberService, never()).register(any(MemberRegisterForm.class));
    }

    /**
     * 完了画面を直アクセスした場合は、登録完了のフラッシュ属性が無いため 404 を返すことを確認する。
     */
    @Test
    void complete_returnsNotFoundWhenFlashAttributesAreMissing() throws Exception {
        mockMvc.perform(get("/members/register/complete"))
                .andExpect(status().isNotFound());
    }
}
