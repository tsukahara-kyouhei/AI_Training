package jp.co.skig.officeorder.model.contact;

/**
 * ログイン会員からお問い合わせフォームへ反映する初期値。
 */
public record ContactMemberPrefill(
        String companyName,
        String departmentName,
        String lastName,
        String firstName,
        String email,
        String phone
) {
}

