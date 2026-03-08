package jp.co.skig.officeorder.model.order;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jp.co.skig.officeorder.common.validation.ValidationPatterns;
import org.springframework.util.StringUtils;

/**
 * 注文情報入力画面のフォームモデル。
 */
public class CheckoutInputForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{validation.personalOrCorporate.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_PERSONAL_OR_CORPORATE, message = "{validation.personalOrCorporate.invalid}")
    private String personalOrCorporate = "personal";

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 50, message = "{validation.lastName.max}")
    private String lastName;

    @NotBlank(message = "{validation.firstName.required}")
    @Size(max = 50, message = "{validation.firstName.max}")
    private String firstName;

    @NotBlank(message = "{validation.lastNameKana.required}")
    @Size(max = 50, message = "{validation.lastNameKana.max}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_KATAKANA, message = "{validation.lastNameKana.pattern}")
    private String lastNameKana;

    @NotBlank(message = "{validation.firstNameKana.required}")
    @Size(max = 50, message = "{validation.firstNameKana.max}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_KATAKANA, message = "{validation.firstNameKana.pattern}")
    private String firstNameKana;

    @Size(max = 120, message = "{validation.companyName.max}")
    private String companyName;

    @Size(max = 120, message = "{validation.departmentName.max}")
    private String departmentName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.format}")
    @Size(max = 254, message = "{validation.email.max}")
    private String email;

    @NotBlank(message = "{validation.daytimePhone.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_PHONE_NUMBER, message = "{validation.daytimePhone.pattern}")
    private String daytimePhone;

    @Pattern(regexp = ValidationPatterns.BLANK_OR_PHONE_NUMBER, message = "{validation.fax.pattern}")
    private String fax;

    @NotBlank(message = "{validation.postalCodePart1.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_POSTAL_CODE_PART1, message = "{validation.postalCodePart1.pattern}")
    private String postalCodePart1;

    @NotBlank(message = "{validation.postalCodePart2.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_POSTAL_CODE_PART2, message = "{validation.postalCodePart2.pattern}")
    private String postalCodePart2;

    @NotBlank(message = "{validation.prefecture.required}")
    @Size(max = 20, message = "{validation.prefecture.max}")
    private String prefecture;

    @NotBlank(message = "{validation.city.required}")
    @Size(max = 120, message = "{validation.city.max}")
    private String city;

    @NotBlank(message = "{validation.addressLine.required}")
    @Size(max = 255, message = "{validation.addressLine.max}")
    private String addressLine;

    @NotBlank(message = "{validation.deliveryFloor.required}")
    @Size(max = 20, message = "{validation.deliveryFloor.max}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_FLOOR_NUMBER, message = "{validation.deliveryFloor.pattern}")
    private String deliveryFloor;

    @NotNull(message = "{validation.hasElevator.required}")
    private Boolean hasElevator = Boolean.TRUE;

    @NotBlank(message = "{validation.paymentMethod.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_PAYMENT_METHOD, message = "{validation.paymentMethod.invalid}")
    private String paymentMethod = "bank_transfer";

    private Long selectedAdditionalAddressId;

    /**
     * 確認画面表示や保存前に入力文字列を trim し、未入力を null に寄せた複製を返す。
     */
    public CheckoutInputForm normalize() {
        CheckoutInputForm normalized = new CheckoutInputForm();
        normalized.personalOrCorporate = trimToNull(personalOrCorporate);
        normalized.lastName = trimToNull(lastName);
        normalized.firstName = trimToNull(firstName);
        normalized.lastNameKana = trimToNull(lastNameKana);
        normalized.firstNameKana = trimToNull(firstNameKana);
        normalized.companyName = trimToNull(companyName);
        normalized.departmentName = trimToNull(departmentName);
        normalized.email = trimToNull(email);
        normalized.daytimePhone = trimToNull(daytimePhone);
        normalized.fax = trimToNull(fax);
        normalized.postalCodePart1 = trimToNull(postalCodePart1);
        normalized.postalCodePart2 = trimToNull(postalCodePart2);
        normalized.prefecture = trimToNull(prefecture);
        normalized.city = trimToNull(city);
        normalized.addressLine = trimToNull(addressLine);
        normalized.deliveryFloor = trimToNull(deliveryFloor);
        normalized.hasElevator = hasElevator;
        normalized.paymentMethod = trimToNull(paymentMethod);
        normalized.selectedAdditionalAddressId = selectedAdditionalAddressId;
        return normalized;
    }

    /**
     * 会社名必須判定などに使う法人注文かどうかを返す。
     */
    public boolean isCorporate() {
        return "corporate".equals(personalOrCorporate);
    }

    /**
     * 注文者区分コードを確認画面向け表示へ変換する。
     */
    public String getPersonalOrCorporateLabel() {
        return isCorporate() ? "法人" : "個人";
    }

    /**
     * エレベーター有無を確認画面向け表示へ変換する。
     */
    public String getHasElevatorLabel() {
        return Boolean.TRUE.equals(hasElevator) ? "あり" : "なし";
    }

    /**
     * 決済手段コードを確認画面向け表示へ変換する。
     */
    public String getPaymentMethodLabel() {
        if ("cash_on_delivery".equals(paymentMethod)) {
            return "代金引換";
        }
        if ("convenience_store".equals(paymentMethod)) {
            return "コンビニ決済";
        }
        return "銀行振込";
    }

    /**
     * 分割入力された郵便番号を DB 保存用の7桁文字列へ連結する。
     */
    public String getPostalCode() {
        String p1 = trimToNull(postalCodePart1);
        String p2 = trimToNull(postalCodePart2);
        if (p1 == null || p2 == null) {
            return null;
        }
        return p1 + p2;
    }

    /**
     * 郵便番号を確認画面向けのハイフン付き表記へ整形する。
     */
    public String getPostalCodeDisplay() {
        String p1 = trimToNull(postalCodePart1);
        String p2 = trimToNull(postalCodePart2);
        if (p1 == null || p2 == null) {
            return "-";
        }
        return p1 + "-" + p2;
    }

    /**
     * 階数入力値に単位を付けて表示用文字列へ変換する。
     */
    public String getDeliveryFloorDisplay() {
        return StringUtils.hasText(deliveryFloor) ? deliveryFloor + "階" : "-";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public String getPersonalOrCorporate() {
        return personalOrCorporate;
    }

    public void setPersonalOrCorporate(String personalOrCorporate) {
        this.personalOrCorporate = personalOrCorporate;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastNameKana() {
        return lastNameKana;
    }

    public void setLastNameKana(String lastNameKana) {
        this.lastNameKana = lastNameKana;
    }

    public String getFirstNameKana() {
        return firstNameKana;
    }

    public void setFirstNameKana(String firstNameKana) {
        this.firstNameKana = firstNameKana;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDaytimePhone() {
        return daytimePhone;
    }

    public void setDaytimePhone(String daytimePhone) {
        this.daytimePhone = daytimePhone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getPostalCodePart1() {
        return postalCodePart1;
    }

    public void setPostalCodePart1(String postalCodePart1) {
        this.postalCodePart1 = postalCodePart1;
    }

    public String getPostalCodePart2() {
        return postalCodePart2;
    }

    public void setPostalCodePart2(String postalCodePart2) {
        this.postalCodePart2 = postalCodePart2;
    }

    public String getPrefecture() {
        return prefecture;
    }

    public void setPrefecture(String prefecture) {
        this.prefecture = prefecture;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getDeliveryFloor() {
        return deliveryFloor;
    }

    public void setDeliveryFloor(String deliveryFloor) {
        this.deliveryFloor = deliveryFloor;
    }

    public Boolean getHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(Boolean hasElevator) {
        this.hasElevator = hasElevator;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getSelectedAdditionalAddressId() {
        return selectedAdditionalAddressId;
    }

    public void setSelectedAdditionalAddressId(Long selectedAdditionalAddressId) {
        this.selectedAdditionalAddressId = selectedAdditionalAddressId;
    }
}
