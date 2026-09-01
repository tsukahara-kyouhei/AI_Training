package jp.co.skig.officeorder.mapper.row;

import java.time.LocalDate;

/**
 * 会員情報変更フォームへ展開するためのプロフィール取得結果。
 */
public record MemberProfileEditMapperRow(
                String personalOrCorporate,
                String lastName,
                String firstName,
                String lastNameKana,
                String firstNameKana,
                String companyName,
                String departmentName,
                String email,
                String gender,
                LocalDate anniversaryDate,
                Boolean newsletterOptIn,
                String postalCode,
                String prefecture,
                String city,
                String addressLine,
                Integer deliveryFloor,
                Boolean hasElevator,
                String daytimePhone,
                String fax) {
}
