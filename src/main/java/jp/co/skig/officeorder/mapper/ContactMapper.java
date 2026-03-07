package jp.co.skig.officeorder.mapper;

import jp.co.skig.officeorder.model.contact.ContactForm;
import jp.co.skig.officeorder.model.contact.ContactMemberPrefill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * お問い合わせ画面の補助データ取得と問い合わせ保存を担う Mapper。
 */
@Mapper
public interface ContactMapper {

    /**
     * ログイン会員のお客様情報から、お問い合わせフォームの初期値を取得する。
     */
    ContactMemberPrefill selectMemberContactPrefill(@Param("memberId") long memberId);

    /**
     * お問い合わせ内容を永続化し、採番された問い合わせIDを返す。
     */
    Long insertInquiry(@Param("memberId") Long memberId,
                       @Param("form") ContactForm form);
}



