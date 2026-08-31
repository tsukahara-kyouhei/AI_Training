package jp.co.skig.officeorder.service.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

        private ContactService service;

        @BeforeEach
        void setUp() {
                service = new ContactService(contactRepository);
        }

        @Test
        void createInitialForm_ゲストなら空フォームを返す() {

                ContactForm result = service.createInitialForm(Optional.empty());

                assertThat(result.getCompanyName()).isNull();
                assertThat(result.getDepartmentName()).isNull();
                assertThat(result.getLastName()).isNull();
                assertThat(result.getFirstName()).isNull();
                assertThat(result.getEmail()).isNull();
                assertThat(result.getPhone()).isNull();

                verify(contactRepository, never())
                                .findMemberPrefill(anyLong());
        }

        @Test
        void createInitialForm_会員情報が存在しないなら空フォームを返す() {

                MemberSessionUser member = new MemberSessionUser(
                                1L,
                                "test@example.com",
                                "山田",
                                "太郎");

                when(contactRepository.findMemberPrefill(1L))
                                .thenReturn(Optional.empty());

                ContactForm result = service.createInitialForm(Optional.of(member));

                assertThat(result.getCompanyName()).isNull();
                assertThat(result.getDepartmentName()).isNull();
                assertThat(result.getLastName()).isNull();
                assertThat(result.getFirstName()).isNull();
                assertThat(result.getEmail()).isNull();
                assertThat(result.getPhone()).isNull();

                verify(contactRepository)
                                .findMemberPrefill(1L);
        }

        @Test
        void createInitialForm_会員情報が存在するならフォームへ設定する() {

                MemberSessionUser member = new MemberSessionUser(
                                1L,
                                "test@example.com",
                                "山田",
                                "太郎");

                ContactMemberPrefill prefill = new ContactMemberPrefill(
                                "株式会社サンプル",
                                "営業部",
                                "佐藤",
                                "花子",
                                "hanako@example.com",
                                "090-1234-5678");

                when(contactRepository.findMemberPrefill(1L))
                                .thenReturn(Optional.of(prefill));

                ContactForm result = service.createInitialForm(Optional.of(member));

                assertThat(result.getCompanyName())
                                .isEqualTo("株式会社サンプル");

                assertThat(result.getDepartmentName())
                                .isEqualTo("営業部");

                assertThat(result.getLastName())
                                .isEqualTo("佐藤");

                assertThat(result.getFirstName())
                                .isEqualTo("花子");

                assertThat(result.getEmail())
                                .isEqualTo("hanako@example.com");

                assertThat(result.getPhone())
                                .isEqualTo("090-1234-5678");

                verify(contactRepository)
                                .findMemberPrefill(1L);
        }

        @Test
        void submit_正常系_問い合わせIDを返す() {

                ContactForm form = createValidForm();

                when(contactRepository.insertInquiry(
                                eq(1L),
                                any(ContactForm.class)))
                                .thenReturn(100L);

                long result = service.submit(1L, form);

                assertThat(result)
                                .isEqualTo(100L);

                verify(contactRepository)
                                .insertInquiry(
                                                eq(1L),
                                                any(ContactForm.class));
        }

        @Test
        void submit_入力値をtrimしてRepositoryへ渡す() {

                ContactForm form = createValidForm();

                form.setCompanyName("  株式会社サンプル  ");
                form.setLastName("  山田  ");
                form.setFirstName("  太郎  ");
                form.setEmail("  test@example.com  ");
                form.setMessage("  お問い合わせ内容です  ");

                when(contactRepository.insertInquiry(eq(1L), any(ContactForm.class)))
                                .thenReturn(100L);

                service.submit(1L, form);

                ArgumentCaptor<ContactForm> captor = ArgumentCaptor.forClass(ContactForm.class);

                verify(contactRepository)
                                .insertInquiry(eq(1L), captor.capture());

                ContactForm savedForm = captor.getValue();

                assertThat(savedForm.getCompanyName())
                                .isEqualTo("株式会社サンプル");

                assertThat(savedForm.getLastName())
                                .isEqualTo("山田");

                assertThat(savedForm.getFirstName())
                                .isEqualTo("太郎");

                assertThat(savedForm.getEmail())
                                .isEqualTo("test@example.com");

                assertThat(savedForm.getMessage())
                                .isEqualTo("お問い合わせ内容です");
        }

        @Test
        void submit_ゲストならmemberIdにnullを渡す() {

                ContactForm form = createValidForm();

                when(contactRepository.insertInquiry(isNull(), any(ContactForm.class)))
                                .thenReturn(100L);

                long result = service.submit(null, form);

                assertThat(result)
                                .isEqualTo(100L);

                verify(contactRepository)
                                .insertInquiry(isNull(), any(ContactForm.class));
        }

        private ContactForm createValidForm() {

                ContactForm form = new ContactForm();

                form.setCompanyName("株式会社サンプル");
                form.setDepartmentName("営業部");
                form.setLastName("山田");
                form.setFirstName("太郎");
                form.setEmail("test@example.com");
                form.setPhone("090-1234-5678");
                form.setInquiryType("product");
                form.setOrderPhase("before_order");
                form.setProductName("ワークデスク");
                form.setProductCode("P0001-C01");
                form.setMessage("お問い合わせ内容です");

                return form;
        }
}