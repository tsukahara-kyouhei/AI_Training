package jp.co.skig.officeorder.service.contact;

import java.util.Optional;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactServiceTest {

    private ContactRepository contactRepository;
    private ContactService sut;

    @BeforeEach
    void setUp() {
        contactRepository = mock(ContactRepository.class);
        sut = new ContactService(contactRepository);
    }

    // --- createInitialForm ---

    @Test
    @DisplayName("ゲスト（会員なし）の場合は空のフォームを返す")
    void createInitialForm_noMember_returnsEmptyForm() {
        ContactForm form = sut.createInitialForm(Optional.empty());

        assertThat(form).isNotNull();
        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    @Test
    @DisplayName("会員のプリフィルが見つかった場合はフォームに会員情報をセットする")
    void createInitialForm_memberFoundPrefill_fillsForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        ContactMemberPrefill prefill = new ContactMemberPrefill(
                "株式会社テスト", "営業部", "山田", "太郎", "test@example.com", "03-1234-5678"
        );
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        ContactForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getCompanyName()).isEqualTo("株式会社テスト");
        assertThat(form.getDepartmentName()).isEqualTo("営業部");
        assertThat(form.getLastName()).isEqualTo("山田");
        assertThat(form.getFirstName()).isEqualTo("太郎");
        assertThat(form.getEmail()).isEqualTo("test@example.com");
        assertThat(form.getPhone()).isEqualTo("03-1234-5678");
    }

    @Test
    @DisplayName("会員のプリフィルが見つからない場合は空のフォームを返す")
    void createInitialForm_memberNoPrefill_returnsEmptyForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.empty());

        ContactForm form = sut.createInitialForm(Optional.of(member));

        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    // --- submit ---

    @Test
    @DisplayName("submit は正規化したフォームを保存し、採番された問い合わせ ID を返す")
    void submit_validForm_returnsInquiryId() {
        ContactForm rawForm = new ContactForm();
        rawForm.setLastName("  山田  ");
        rawForm.setFirstName("太郎");
        rawForm.setEmail("test@example.com");
        rawForm.setMessage("テスト問い合わせ");
        when(contactRepository.insertInquiry(eq(null), any(ContactForm.class))).thenReturn(42L);

        long result = sut.submit(null, rawForm);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("submit はフォームを normalize してからリポジトリに渡す")
    void submit_trimsFormBeforeInsert() {
        ContactForm rawForm = new ContactForm();
        rawForm.setLastName("  山田  ");
        rawForm.setFirstName("太郎");
        rawForm.setEmail("test@example.com");
        rawForm.setMessage("テスト");
        when(contactRepository.insertInquiry(any(), any())).thenReturn(1L);

        sut.submit(null, rawForm);

        ArgumentCaptor<ContactForm> captor = ArgumentCaptor.forClass(ContactForm.class);
        verify(contactRepository).insertInquiry(eq(null), captor.capture());
        assertThat(captor.getValue().getLastName()).isEqualTo("山田");
    }
}
