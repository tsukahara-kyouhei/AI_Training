package jp.co.skig.officeorder.model.member;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

/**
 * 会員登録とデフォルトお届け先登録をまとめて受け持つフォームモデル。
 */
public class MemberRegisterForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{validation.personalOrCorporate.required}")
    @Pattern(regexp = "personal|corporate", message = "{validation.personalOrCorporate.invalid}")
    private String personalOrCorporate = "personal";

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 50, message = "{validation.lastName.max}")
    private String lastName;

    @NotBlank(message = "{validation.firstName.required}")
    @Size(max = 50, message = "{validation.firstName.max}")
    private String firstName;

    @NotBlank(message = "{validation.lastNameKana.required}")
    @Size(max = 50, message = "{validation.lastNameKana.max}")
    @Pattern(regexp = "^[ァ-ヶー]+$", message = "{validation.lastNameKana.pattern}")
    private String lastNameKana;

    @NotBlank(message = "{validation.firstNameKana.required}")
    @Size(max = 50, message = "{validation.firstNameKana.max}")
    @Pattern(regexp = "^[ァ-ヶー]+$", message = "{validation.firstNameKana.pattern}")
    private String firstNameKana;

    @Size(max = 120, message = "{validation.companyName.max}")
    private String companyName;

    @Size(max = 120, message = "{validation.departmentName.max}")
    private String departmentName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.format}")
    @Size(max = 254, message = "{validation.email.max}")
    private String email;

    @NotBlank(message = "{validation.gender.required}")
    @Pattern(regexp = "male|female|no_answer", message = "{validation.gender.invalid}")
    private String gender = "male";

    @NotNull(message = "{validation.anniversaryDate.required}")
    private LocalDate anniversaryDate;

    @NotBlank(message = "{validation.password.required}")
    @Pattern(regexp = "^[\\x21-\\x7E]{8,64}$", message = "{validation.password.register.format}")
    private String password;

    @NotNull(message = "{validation.newsletterOptIn.required}")
    private Boolean newsletterOptIn = Boolean.TRUE;

    @NotBlank(message = "{validation.postalCodePart1.required}")
    @Pattern(regexp = "^[0-9]{3}$", message = "{validation.postalCodePart1.pattern}")
    private String postalCodePart1;

    @NotBlank(message = "{validation.postalCodePart2.required}")
    @Pattern(regexp = "^[0-9]{4}$", message = "{validation.postalCodePart2.pattern}")
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
    @Pattern(regexp = "^[0-9]+$", message = "{validation.deliveryFloor.pattern}")
    private String deliveryFloor;

    @NotNull(message = "{validation.hasElevator.required}")
    private Boolean hasElevator = Boolean.TRUE;

    @NotBlank(message = "{validation.daytimePhone.required}")
    @Pattern(regexp = "^[0-9]{10,12}$", message = "{validation.daytimePhone.pattern}")
    private String daytimePhone;

    @Pattern(regexp = "^$|^[0-9]{10,12}$", message = "{validation.fax.pattern}")
    private String fax;

    /**
     * 確認画面表示や保存前に入力文字列を trim し、未入力を null に寄せた複製を返す。
     */
    public MemberRegisterForm normalize() {
        MemberRegisterForm normalized = new MemberRegisterForm();
        normalized.personalOrCorporate = trimToNull(personalOrCorporate);
        normalized.lastName = trimToNull(lastName);
        normalized.firstName = trimToNull(firstName);
        normalized.lastNameKana = trimToNull(lastNameKana);
        normalized.firstNameKana = trimToNull(firstNameKana);
        normalized.companyName = trimToNull(companyName);
        normalized.departmentName = trimToNull(departmentName);
        normalized.email = trimToNull(email);
        normalized.gender = trimToNull(gender);
        normalized.anniversaryDate = anniversaryDate;
        normalized.password = password;
        normalized.newsletterOptIn = newsletterOptIn;
        normalized.postalCodePart1 = trimToNull(postalCodePart1);
        normalized.postalCodePart2 = trimToNull(postalCodePart2);
        normalized.prefecture = trimToNull(prefecture);
        normalized.city = trimToNull(city);
        normalized.addressLine = trimToNull(addressLine);
        normalized.deliveryFloor = trimToNull(deliveryFloor);
        normalized.hasElevator = hasElevator;
        normalized.daytimePhone = trimToNull(daytimePhone);
        normalized.fax = trimToNull(fax);
        return normalized;
    }

    /**
     * 会社名必須判定などに使う法人会員かどうかを返す。
     */
    public boolean isCorporate() {
        return "corporate".equals(personalOrCorporate);
    }

    /**
     * 会員種別コードを確認画面向けの表示文言へ変換する。
     */
    public String getPersonalOrCorporateLabel() {
        return isCorporate() ? "法人" : "個人";
    }

    /**
     * 性別コードを確認画面向けの表示文言へ変換する。
     */
    public String getGenderLabel() {
        if ("female".equals(gender)) {
            return "女性";
        }
        if ("no_answer".equals(gender)) {
            return "無回答";
        }
        return "男性";
    }

    /**
     * メールマガジン希望有無を表示文言へ変換する。
     */
    public String getNewsletterOptInLabel() {
        return Boolean.TRUE.equals(newsletterOptIn) ? "希望する" : "希望しない";
    }

    /**
     * エレベーター有無を表示文言へ変換する。
     */
    public String getHasElevatorLabel() {
        return Boolean.TRUE.equals(hasElevator) ? "あり" : "なし";
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getAnniversaryDate() {
        return anniversaryDate;
    }

    public void setAnniversaryDate(LocalDate anniversaryDate) {
        this.anniversaryDate = anniversaryDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getNewsletterOptIn() {
        return newsletterOptIn;
    }

    public void setNewsletterOptIn(Boolean newsletterOptIn) {
        this.newsletterOptIn = newsletterOptIn;
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

