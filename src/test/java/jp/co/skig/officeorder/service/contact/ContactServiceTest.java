package jp.co.skig.officeorder.service.contact;

import java.util.Optional;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    private ContactService sut;

    @BeforeEach
    void setUp() {
        sut = new ContactService(contactRepository);
    }

    // ─── createInitialForm ──────────────────────────────────────────────

    @Test
    void createInitialForm_guest_returns_empty_form() {
        // Arrange / Act
        ContactForm result = sut.createInitialForm(Optional.empty());

        // Assert
        assertThat(result.getLastName()).isNull();
        assertThat(result.getFirstName()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void createInitialForm_member_but_prefill_not_found_returns_empty_form() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.empty());

        // Act
        ContactForm result = sut.createInitialForm(Optional.of(member));

        // Assert
        assertThat(result.getLastName()).isNull();
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void createInitialForm_member_with_prefill_populates_form_fields() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        ContactMemberPrefill prefill = new ContactMemberPrefill(
                "株式会社テスト", "営業部", "山田", "太郎", "test@example.com", "0312345678"
        );
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        // Act
        ContactForm result = sut.createInitialForm(Optional.of(member));

        // Assert
        assertThat(result.getCompanyName()).isEqualTo("株式会社テスト");
        assertThat(result.getDepartmentName()).isEqualTo("営業部");
        assertThat(result.getLastName()).isEqualTo("山田");
        assertThat(result.getFirstName()).isEqualTo("太郎");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhone()).isEqualTo("0312345678");
    }

    @Test
    void createInitialForm_member_with_null_optional_fields_in_prefill_sets_null_fields() {
        // Arrange
        MemberSessionUser member = new MemberSessionUser(2L, "guest@example.com", "鈴木", "花子");
        ContactMemberPrefill prefill = new ContactMemberPrefill(
                null, null, "鈴木", "花子", "guest@example.com", null
        );
        when(contactRepository.findMemberPrefill(2L)).thenReturn(Optional.of(prefill));

        // Act
        ContactForm result = sut.createInitialForm(Optional.of(member));

        // Assert
        assertThat(result.getCompanyName()).isNull();
        assertThat(result.getDepartmentName()).isNull();
        assertThat(result.getPhone()).isNull();
        assertThat(result.getLastName()).isEqualTo("鈴木");
        assertThat(result.getEmail()).isEqualTo("guest@example.com");
    }

    // ─── submit ────────────────────────────────────────────────────────

    @Test
    void submit_guest_passes_null_member_id_to_repository() {
        // Arrange
        ContactForm form = buildContactForm();
        when(contactRepository.insertInquiry(any(), any())).thenReturn(100L);

        // Act
        long inquiryId = sut.submit(null, form);

        // Assert
        assertThat(inquiryId).isEqualTo(100L);
        verify(contactRepository).insertInquiry(eq(null), any());
    }

    @Test
    void submit_member_passes_member_id_to_repository() {
        // Arrange
        ContactForm form = buildContactForm();
        when(contactRepository.insertInquiry(eq(42L), any())).thenReturn(200L);

        // Act
        long inquiryId = sut.submit(42L, form);

        // Assert
        assertThat(inquiryId).isEqualTo(200L);
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private ContactForm buildContactForm() {
        ContactForm form = new ContactForm();
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setEmail("test@example.com");
        form.setInquiryType("product");
        form.setOrderPhase("before_order");
        form.setMessage("テストメッセージ");
        return form;
    }
}
