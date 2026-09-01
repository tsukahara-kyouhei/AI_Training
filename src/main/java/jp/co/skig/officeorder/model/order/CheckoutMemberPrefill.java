package jp.co.skig.officeorder.model.order;

/**
 * ログイン会員の情報から注文入力フォームへ反映する初期値。
 */
public record CheckoutMemberPrefill(
                String personalOrCorporate,
                String lastName,
                String firstName,
                String lastNameKana,
                String firstNameKana,
                String companyName,
                String departmentName,
                String email,
                String daytimePhone,
                String fax,
                String postalCode,
                String prefecture,
                String city,
                String addressLine,
                Integer deliveryFloor,
                Boolean hasElevator) {
}
