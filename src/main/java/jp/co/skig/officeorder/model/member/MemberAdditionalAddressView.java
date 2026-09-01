package jp.co.skig.officeorder.model.member;

import java.time.OffsetDateTime;

/**
 * 追加お届け先一覧や注文入力の選択肢に表示する住所情報。
 */
public record MemberAdditionalAddressView(
        Long memberAddressId,
        Long memberId,
        String lastName,
        String firstName,
        String lastNameKana,
        String firstNameKana,
        String companyName,
        String departmentName,
        String postalCode,
        String prefecture,
        String city,
        String addressLine,
        Integer deliveryFloor,
        Boolean hasElevator,
        String daytimePhone,
        String fax,
        OffsetDateTime createdAt) {
    /**
     * 宛名の姓・名を画面表示用に連結する。
     */
    public String fullName() {
        return lastName + " " + firstName;
    }

    /**
     * 郵便番号をハイフン付き表示へ変換する。
     */
    public String postalCodeDisplay() {
        if (postalCode == null || postalCode.length() != 7) {
            return postalCode;
        }
        return postalCode.substring(0, 3) + "-" + postalCode.substring(3);
    }

    /**
     * 住所選択プルダウンで識別しやすい要約文字列を返す。
     */
    public String addressSummary() {
        return "〒" + postalCode + " " + prefecture + city + addressLine + "（" + deliveryFloor + "F/EV"
                + (Boolean.TRUE.equals(hasElevator) ? "あり" : "なし") + "）";
    }
}
