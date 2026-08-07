package jp.co.skig.officeorder.service.contact;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactServiceTest {

    private final ContactRepository repository = Mockito.mock(ContactRepository.class);
    private final ContactService service = new ContactService(repository);

    @Test
    void createInitialForm_withoutMember_shouldReturnEmptyForm() {
        ContactForm form = service.createInitialForm(Optional.empty());

        assertNull(form.getCompanyName());
        assertNull(form.getLastName());
        assertNull(form.getEmail());
    }

    @Test
    void createInitialForm_withMemberPrefill_shouldPopulateForm() {
        MemberSessionUser member = new MemberSessionUser(123L, "user@example.com", "山田", "太郎");
        ContactMemberPrefill prefill = new ContactMemberPrefill("株式会社", "営業部", "山田", "太郎", "user@example.com",
                "030-1234-5678");
        when(repository.findMemberPrefill(member.memberId())).thenReturn(Optional.of(prefill));

        ContactForm form = service.createInitialForm(Optional.of(member));

        assertEquals("株式会社", form.getCompanyName());
        assertEquals("営業部", form.getDepartmentName());
        assertEquals("山田", form.getLastName());
        assertEquals("太郎", form.getFirstName());
        assertEquals("user@example.com", form.getEmail());
        assertEquals("030-1234-5678", form.getPhone());
    }

    @Test
    void submit_shouldNormalizeAndInsertContact() {
        ContactForm raw = new ContactForm();
        raw.setCompanyName(" Test ");
        raw.setDepartmentName(" Dept ");
        raw.setLastName(" Yamada ");
        raw.setFirstName(" Taro ");
        raw.setEmail(" user@example.com ");
        raw.setPhone(" 030-1234-5678 ");
        raw.setInquiryType("product");
        raw.setOrderPhase("before_order");
        raw.setMessage("Hello");
        when(repository.insertInquiry(any(Long.class), any(ContactForm.class))).thenReturn(123L);

        long inquiryId = service.submit(321L, raw);

        assertEquals(123L, inquiryId);
        verify(repository).insertInquiry(321L, raw.normalize());
    }
}
