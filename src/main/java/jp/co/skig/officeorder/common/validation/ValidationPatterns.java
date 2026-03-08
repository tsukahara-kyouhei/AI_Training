package jp.co.skig.officeorder.common.validation;

/**
 * フォーム入力検証で使い回す正規表現を集約する定数クラス。
 *
 * <p>同じ検証ルールを複数フォームで共有する場合はここへ集約し、
 * 形式変更時の修正漏れを防ぐ。
 */
public final class ValidationPatterns {

    /** 未入力または個人/法人区分コードを許容する。 */
    public static final String BLANK_OR_PERSONAL_OR_CORPORATE = "^$|^(personal|corporate)$";
    /** 未入力または性別コードを許容する。 */
    public static final String BLANK_OR_GENDER = "^$|^(male|female|no_answer)$";
    /** 未入力または全角カタカナを許容する。 */
    public static final String BLANK_OR_KATAKANA = "^$|^[ァ-ヶー]+$";
    /** 未入力または半角数字3桁を許容する。 */
    public static final String BLANK_OR_POSTAL_CODE_PART1 = "^$|^[0-9]{3}$";
    /** 未入力または半角数字4桁を許容する。 */
    public static final String BLANK_OR_POSTAL_CODE_PART2 = "^$|^[0-9]{4}$";
    /** 未入力または半角数字のみの階数入力を許容する。 */
    public static final String BLANK_OR_FLOOR_NUMBER = "^$|^[0-9]+$";
    /** 未入力または半角数字10〜12桁の電話番号/FAXを許容する。 */
    public static final String BLANK_OR_PHONE_NUMBER = "^$|^[0-9]{10,12}$";
    /** 未入力または会員登録用パスワード形式を許容する。 */
    public static final String BLANK_OR_REGISTER_PASSWORD = "^$|^[\\x21-\\x7E]{8,64}$";
    /** 未入力または決済方法コードを許容する。 */
    public static final String BLANK_OR_PAYMENT_METHOD =
            "^$|^(bank_transfer|cash_on_delivery|convenience_store)$";
    /** 未入力またはお問い合わせ種別コードを許容する。 */
    public static final String BLANK_OR_INQUIRY_TYPE =
            "^$|^(product|delivery_date|order|shipping|return_cancel|other)$";
    /** 未入力または注文前後区分コードを許容する。 */
    public static final String BLANK_OR_ORDER_PHASE = "^$|^(before_order|after_order)$";

    private ValidationPatterns() {
    }
}
