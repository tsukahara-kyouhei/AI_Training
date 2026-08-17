package jp.co.skig.officeorder.service.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Nested
    @DisplayName("createInitialFormのテスト")
    class CreateInitialFormTest {

        @Test
        @DisplayName("未ログイン（ゲスト）の場合、空のフォームが返ること")
        void shouldReturnEmptyFormWhenGuest() {
            ContactForm form = contactService.createInitialForm(Optional.empty());

            assertThat(form).isNotNull();
            assertThat(form.getEmail()).isNull();
            assertThat(form.getLastName()).isNull();
        }

        @Test
        @DisplayName("ログイン会員で事前入力情報が存在する場合、会員情報が反映されたフォームが返ること")
        void shouldReturnPrefilledFormWhenMemberExists() {
            long memberId = 100L;
            MemberSessionUser member = new MemberSessionUser(memberId, "test@example.com", "テスト", "太郎");
            ContactMemberPrefill prefill = new ContactMemberPrefill(
                    "株式会社テスト", "開発部", "山田", "太郎", "yamada@example.com", "090-1234-5678"
            );

            when(contactRepository.findMemberPrefill(memberId)).thenReturn(Optional.of(prefill));

            ContactForm form = contactService.createInitialForm(Optional.of(member));

            assertThat(form).isNotNull();
            assertThat(form.getCompanyName()).isEqualTo("株式会社テスト");
            assertThat(form.getDepartmentName()).isEqualTo("開発部");
            assertThat(form.getLastName()).isEqualTo("山田");
            assertThat(form.getFirstName()).isEqualTo("太郎");
            assertThat(form.getEmail()).isEqualTo("yamada@example.com");
            assertThat(form.getPhone()).isEqualTo("090-1234-5678");
        }

        @Test
        @DisplayName("ログイン会員だが事前入力情報が見つからない場合、空のフォームが返ること")
        void shouldReturnEmptyFormWhenMemberPrefillNotFound() {
            long memberId = 200L;
            MemberSessionUser member = new MemberSessionUser(memberId, "none@example.com", "名無し", "権兵衛");

            when(contactRepository.findMemberPrefill(memberId)).thenReturn(Optional.empty());

            ContactForm form = contactService.createInitialForm(Optional.of(member));

            assertThat(form).isNotNull();
            assertThat(form.getEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("submitのテスト")
    class SubmitTest {

        @Test
        @DisplayName("問い合わせを正常に登録し、採番されたIDを返すこと")
        void shouldSubmitContactSuccessfully() {
            Long memberId = 100L;
            ContactForm rawForm = new ContactForm();
            rawForm.setEmail(" test@example.com ");
            rawForm.setLastName(" 山田 ");

            when(contactRepository.insertInquiry(eq(memberId), any(ContactForm.class))).thenReturn(1L);

            long resultId = contactService.submit(memberId, rawForm);

            assertThat(resultId).isEqualTo(1L);
            verify(contactRepository).insertInquiry(eq(memberId), any(ContactForm.class));
        }
    }
}