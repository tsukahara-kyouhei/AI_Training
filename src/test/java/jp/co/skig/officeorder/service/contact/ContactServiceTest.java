package jp.co.skig.officeorder.service.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    ContactRepository contactRepository;

    @InjectMocks
    ContactService contactService;

    // --- createInitialForm ---

    @Test
    void createInitialForm_noMember_returnsEmptyForm() {
        ContactForm form = contactService.createInitialForm(Optional.empty());

        assertThat(form).isNotNull();
        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    @Test
    void createInitialForm_memberWithPrefill_returnsPrefilledForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "taro@example.com", "山田", "太郎");
        ContactMemberPrefill prefill = new ContactMemberPrefill(
                "株式会社テスト", "営業部", "山田", "太郎", "taro@example.com", "0312345678");
        when(contactRepository.findMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        ContactForm form = contactService.createInitialForm(Optional.of(member));

        assertThat(form.getLastName()).isEqualTo("山田");
        assertThat(form.getFirstName()).isEqualTo("太郎");
        assertThat(form.getEmail()).isEqualTo("taro@example.com");
        assertThat(form.getCompanyName()).isEqualTo("株式会社テスト");
        assertThat(form.getDepartmentName()).isEqualTo("営業部");
        assertThat(form.getPhone()).isEqualTo("0312345678");
    }

    @Test
    void createInitialForm_memberWithNoPrefill_returnsEmptyForm() {
        MemberSessionUser member = new MemberSessionUser(2L, "hanako@example.com", "佐藤", "花子");
        when(contactRepository.findMemberPrefill(2L)).thenReturn(Optional.empty());

        ContactForm form = contactService.createInitialForm(Optional.of(member));

        assertThat(form.getLastName()).isNull();
        assertThat(form.getEmail()).isNull();
    }

    // --- submit ---

    @Test
    void submit_guest_savesWithNullMemberIdAndReturnsId() {
        ContactForm form = buildValidContactForm();
        when(contactRepository.insertInquiry(isNull(), any())).thenReturn(42L);

        long result = contactService.submit(null, form);

        assertThat(result).isEqualTo(42L);
        verify(contactRepository).insertInquiry(isNull(), any());
    }

    @Test
    void submit_member_savesWithMemberIdAndReturnsId() {
        ContactForm form = buildValidContactForm();
        when(contactRepository.insertInquiry(eq(1L), any())).thenReturn(43L);

        long result = contactService.submit(1L, form);

        assertThat(result).isEqualTo(43L);
        verify(contactRepository).insertInquiry(eq(1L), any());
    }

    // --- helpers ---

    private ContactForm buildValidContactForm() {
        ContactForm form = new ContactForm();
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setEmail("taro@example.com");
        form.setPhone("0312345678");
        return form;
    }
}
