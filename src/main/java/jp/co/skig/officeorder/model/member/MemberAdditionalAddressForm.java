package jp.co.skig.officeorder.model.member;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jp.co.skig.officeorder.common.validation.ValidationPatterns;
import org.springframework.util.StringUtils;

/**
 * 会員追加お届け先の登録・変更フォームモデル。
 */
public class MemberAdditionalAddressForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Boolean hasElevator = Boolean.TRUE;

    @NotBlank(message = "{validation.daytimePhone.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_PHONE_NUMBER, message = "{validation.daytimePhone.pattern}")
    private String daytimePhone;

    @Pattern(regexp = ValidationPatterns.BLANK_OR_PHONE_NUMBER, message = "{validation.fax.pattern}")
    private String fax;

    /**
     * 保存前に入力文字列を trim し、未入力を null に寄せた複製を返す。
     */
    public MemberAdditionalAddressForm normalize() {
        MemberAdditionalAddressForm normalized = new MemberAdditionalAddressForm();
        normalized.lastName = trimToNull(lastName);
        normalized.firstName = trimToNull(firstName);
        normalized.lastNameKana = trimToNull(lastNameKana);
        normalized.firstNameKana = trimToNull(firstNameKana);
        normalized.companyName = trimToNull(companyName);
        normalized.departmentName = trimToNull(departmentName);
        normalized.postalCodePart1 = trimToNull(postalCodePart1);
        normalized.postalCodePart2 = trimToNull(postalCodePart2);
        normalized.prefecture = trimToNull(prefecture);
        normalized.city = trimToNull(city);
        normalized.addressLine = trimToNull(addressLine);
        normalized.deliveryFloor = trimToNull(deliveryFloor);
        normalized.hasElevator = hasElevator == null ? Boolean.FALSE : hasElevator;
        normalized.daytimePhone = trimToNull(daytimePhone);
        normalized.fax = trimToNull(fax);
        return normalized;
    }

    /**
     * 分割入力された郵便番号を DB 保存用の7桁文字列へ連結する。
     */
    public String getPostalCode() {
        if (!StringUtils.hasText(postalCodePart1) || !StringUtils.hasText(postalCodePart2)) {
            return null;
        }
        return postalCodePart1.trim() + postalCodePart2.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
}
