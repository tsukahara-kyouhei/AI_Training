package jp.co.skig.officeorder.repository;

import java.util.Optional;

import jp.co.skig.officeorder.mapper.ContactMapper;
import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import org.springframework.stereotype.Repository;

/**
 * お問い合わせ入力補完と問い合わせ保存を担当するリポジトリ。
 */
@Repository
public class ContactRepository {

    /** お問い合わせSQLを呼び出す MyBatis Mapper。 */
    private final ContactMapper contactMapper;

    /**
     * お問い合わせリポジトリを生成する。
     *
     * @param contactMapper お問い合わせMapper
     */
    public ContactRepository(ContactMapper contactMapper) {
        this.contactMapper = contactMapper;
    }

    /**
     * ログイン会員向けのお問い合わせ初期表示情報を取得する。
     *
     * @param memberId 会員ID
     * @return 初期表示情報
     */
    public Optional<ContactMemberPrefill> findMemberPrefill(long memberId) {
        ContactMemberPrefill row = contactMapper.selectMemberContactPrefill(memberId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(row);
    }

    /**
     * お問い合わせを保存する。
     *
     * @param memberId 会員ID。ゲスト時は {@code null}
     * @param form 保存対象フォーム
     * @return 採番された問い合わせID
     */
    public long insertInquiry(Long memberId, ContactForm form) {
        Long inquiryId = contactMapper.insertInquiry(memberId, form);
        if (inquiryId == null) {
            throw new IllegalStateException("お問い合わせの保存に失敗しました。");
        }
        return inquiryId;
    }
}




