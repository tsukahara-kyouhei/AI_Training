package jp.co.skig.officeorder.web;

import java.util.Optional;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.service.announcement.AnnouncementService;
import jp.co.skig.officeorder.service.cart.CartService;
import jp.co.skig.officeorder.service.contact.ContactService;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import jp.co.skig.officeorder.web.view.AssetVersionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ContactController} の画面遷移と利用者向けメッセージを確認するテスト。
 */
@WebMvcTest(controllers = ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private MemberSessionService memberSessionService;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean(name = "assetVersion")
    private AssetVersionResolver assetVersionResolver;

    /**
     * お問い合わせ送信成功時は、受付完了メッセージと問い合わせ番号を flash 属性へ積んで再表示へ戻すことを確認する。
     */
    @Test
    void submit_setsSuccessFlashMessageWhenInquiryIsAccepted() throws Exception {
        when(memberSessionService.currentMember(any())).thenReturn(Optional.empty());
        when(contactService.submit(any(), any(ContactForm.class))).thenReturn(123L);

                mockMvc.perform(post("/contact")
                        .param("companyName", "")
                        .param("departmentName", "")
                        .param("lastName", "山田")
                        .param("firstName", "花子")
                        .param("email", "guest@example.com")
                        .param("phone", "0312345678")
                        .param("inquiryType", "product")
                        .param("orderPhase", "before_order")
                        .param("productName", "")
                        .param("productCode", "")
                        .param("message", "在庫状況を教えてください。"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"))
                .andExpect(flash().attribute("contactSuccessMessage", "お問い合わせを受け付けました。"))
                .andExpect(flash().attribute("contactInquiryNumber", "INQ00000123"));
    }
}
