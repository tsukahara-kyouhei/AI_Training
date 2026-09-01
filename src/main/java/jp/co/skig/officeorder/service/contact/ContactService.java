package jp.co.skig.officeorder.service.contact;

import java.util.Optional;

import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * お問い合わせ入力の初期化と送信を扱うサービス。
 *
 * <p>
 * ログイン会員が問い合わせる場合だけ会員情報を初期表示へ反映し、
 * 送信時は正規化済みフォームを永続化する。
 */
@Service
public class ContactService {

    /** お問い合わせ送信ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    /** お問い合わせ情報の取得・保存を担当するリポジトリ。 */
    private final ContactRepository contactRepository;

    /**
     * お問い合わせサービスを生成する。
     *
     * @param contactRepository お問い合わせリポジトリ
     */
    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * お問い合わせ画面の初期フォームを生成する。
     *
     * <p>
     * ログイン会員の場合は、氏名や連絡先など会員情報から初期値を補完する。
     *
     * @param member ログイン会員
     * @return 初期フォーム
     */
    public ContactForm createInitialForm(Optional<MemberSessionUser> member) {
        ContactForm form = new ContactForm();
        if (member.isEmpty()) {
            return form;
        }
        Optional<ContactMemberPrefill> prefill = contactRepository.findMemberPrefill(member.get().memberId());
        if (prefill.isEmpty()) {
            return form;
        }
        ContactMemberPrefill source = prefill.get();
        form.setCompanyName(source.companyName());
        form.setDepartmentName(source.departmentName());
        form.setLastName(source.lastName());
        form.setFirstName(source.firstName());
        form.setEmail(source.email());
        form.setPhone(source.phone());
        return form;
    }

    /**
     * お問い合わせを送信する。
     *
     * @param memberId ログイン会員ID。ゲスト時は {@code null}
     * @param rawForm  送信フォーム
     * @return 採番された問い合わせID
     */
    @Transactional
    public long submit(Long memberId, ContactForm rawForm) {
        ContactForm form = rawForm.normalize();
        long inquiryId = contactRepository.insertInquiry(memberId, form);
        log.info("event={} inquiryId={} memberId={} inquiryType={} orderPhase={}",
                LogEvent.CONTACT_SUBMITTED.value(),
                inquiryId,
                memberId,
                form.getInquiryType(),
                form.getOrderPhase());
        return inquiryId;
    }
}
