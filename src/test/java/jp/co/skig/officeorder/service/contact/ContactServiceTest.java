package jp.co.skig.officeorder.service.contact;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ContactService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    private ContactService sut;

    @BeforeEach
    void setUp() {
        sut = new ContactService(contactRepository);
    }

    // =========================================================
    // createInitialForm
    // =========================================================

    // ---- CI-01: ゲスト → 空フォームを返す ----
    @Test
    @DisplayName("ゲストには空の初期フォームを返す")
    void ci01_guest_returnsEmptyForm() {
        ContactForm result = sut.createInitialForm(Optional.empty());

        assertThat(result).isNotNull();
        assertThat(result.getLastName()).isNull();
    }

    // ---- CI-02: ログイン会員・Prefill なし → 空フォームを返す ----
    @Test
    @DisplayName("会員情報プリフィルが取得できない場合は空フォームを返す")
    void ci02_memberNoPrefill_returnsEmptyForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.empty());

        ContactForm result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getLastName()).isNull();
    }

    // ---- CI-03: ログイン会員・Prefill あり → 会員情報が反映される ----
    @Test
    @DisplayName("会員情報プリフィルがある場合はフォームに反映される")
    void ci03_memberWithPrefill_formPopulated() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        ContactMemberPrefill prefill = new ContactMemberPrefill(
                null, null, "山田", "太郎", "test@example.com", "0312345678"
        );
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        ContactForm result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getLastName()).isEqualTo("山田");
        assertThat(result.getFirstName()).isEqualTo("太郎");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhone()).isEqualTo("0312345678");
    }

    // =========================================================
    // submit
    // =========================================================

    // ---- CS-01: submit でリポジトリに保存され採番IDを返す ----
    @Test
    @DisplayName("問い合わせ送信でリポジトリに保存され採番IDを返す")
    void cs01_submit_savesToRepositoryAndReturnsId() {
        ContactForm form = makeContactForm();
        when(contactRepository.insertInquiry(any(), any())).thenReturn(10L);

        long result = sut.submit(null, form);

        assertThat(result).isEqualTo(10L);
        verify(contactRepository).insertInquiry(any(), any());
    }

    // ---- CS-02: ゲスト（memberId=null）で submit できる ----
    @Test
    @DisplayName("ゲスト（memberId=null）での問い合わせ送信が正常に完了する")
    void cs02_submitAsGuest_succeeds() {
        ContactForm form = makeContactForm();
        when(contactRepository.insertInquiry(any(), any())).thenReturn(5L);

        long result = sut.submit(null, form);

        assertThat(result).isEqualTo(5L);
    }

    // ---- CS-03: ログイン会員（memberId あり）で submit できる ----
    @Test
    @DisplayName("ログイン会員での問い合わせ送信が正常に完了する")
    void cs03_submitAsMember_succeeds() {
        ContactForm form = makeContactForm();
        when(contactRepository.insertInquiry(any(), any())).thenReturn(7L);

        long result = sut.submit(1L, form);

        assertThat(result).isEqualTo(7L);
        verify(contactRepository).insertInquiry(any(), any());
    }

    // =========================================================
    // helpers
    // =========================================================

    private ContactForm makeContactForm() {
        ContactForm form = new ContactForm();
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setEmail("test@example.com");
        form.setPhone("0312345678");
        form.setInquiryType("general");
        form.setOrderPhase("before_order");
        form.setMessage("お問い合わせ内容です。");
        return form;
    }
}
