package jp.co.skig.officeorder.service.contact;

import java.util.Optional;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ContactService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @InjectMocks
    ContactService sut;

    @Mock
    ContactRepository contactRepository;

    // ── createInitialForm ────────────────────────────────────────────────

    @Test
    void ゲスト会員の場合に空フォームが返ること() {
        var result = sut.createInitialForm(Optional.empty());

        assertThat(result).isNotNull();
        assertThat(result.getLastName()).isNull();
        assertThat(result.getFirstName()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void ログイン会員でプリフィルが存在しない場合に空フォームが返ること() {
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.empty());

        var result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getLastName()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void ログイン会員でプリフィルが存在する場合にフォームへ反映されること() {
        var member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        var prefill = new ContactMemberPrefill("株式会社テスト", "営業部", "山田", "太郎", "test@example.com", "0312345678");
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        var result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getCompanyName()).isEqualTo("株式会社テスト");
        assertThat(result.getDepartmentName()).isEqualTo("営業部");
        assertThat(result.getLastName()).isEqualTo("山田");
        assertThat(result.getFirstName()).isEqualTo("太郎");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhone()).isEqualTo("0312345678");
    }

    @Test
    void プリフィルの一部フィールドがnullの場合もフォームへ反映されること() {
        var member = new MemberSessionUser(2L, "member@example.com", "鈴木", "花子");
        var prefill = new ContactMemberPrefill(null, null, "鈴木", "花子", "member@example.com", null);
        when(contactRepository.findMemberPrefill(2L)).thenReturn(Optional.of(prefill));

        var result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getCompanyName()).isNull();
        assertThat(result.getLastName()).isEqualTo("鈴木");
        assertThat(result.getEmail()).isEqualTo("member@example.com");
        assertThat(result.getPhone()).isNull();
    }

    // ── submit ───────────────────────────────────────────────────────────

    @Test
    void 送信するとリポジトリが呼ばれて問い合わせIDが返ること() {
        when(contactRepository.insertInquiry(eq(1L), any(ContactForm.class))).thenReturn(100L);

        var form = buildContactForm();
        long result = sut.submit(1L, form);

        assertThat(result).isEqualTo(100L);
        verify(contactRepository).insertInquiry(eq(1L), any(ContactForm.class));
    }

    @Test
    void ゲスト送信のときmemberIdにnullを渡してリポジトリが呼ばれること() {
        when(contactRepository.insertInquiry(eq(null), any(ContactForm.class))).thenReturn(200L);

        long result = sut.submit(null, buildContactForm());

        assertThat(result).isEqualTo(200L);
        verify(contactRepository).insertInquiry(eq(null), any(ContactForm.class));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private ContactForm buildContactForm() {
        var form = new ContactForm();
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setEmail("test@example.com");
        form.setInquiryType("product");
        form.setOrderPhase("before_order");
        form.setMessage("テストメッセージ");
        return form;
    }
}
