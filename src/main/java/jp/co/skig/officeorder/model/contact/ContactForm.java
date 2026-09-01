package jp.co.skig.officeorder.model.contact;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jp.co.skig.officeorder.common.validation.ValidationPatterns;
import org.springframework.util.StringUtils;

/**
 * お問い合わせ入力画面のフォームモデル。
 */
public class ContactForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 120, message = "{validation.companyName.max}")
    private String companyName;

    @Size(max = 120, message = "{validation.departmentName.max}")
    private String departmentName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 50, message = "{validation.lastName.max}")
    private String lastName;

    @NotBlank(message = "{validation.firstName.required}")
    @Size(max = 50, message = "{validation.firstName.max}")
    private String firstName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.format}")
    @Size(max = 254, message = "{validation.email.max}")
    private String email;

    @Pattern(regexp = ValidationPatterns.BLANK_OR_PHONE_NUMBER, message = "{validation.daytimePhone.pattern}")
    private String phone;

    @NotBlank(message = "{validation.inquiryType.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_INQUIRY_TYPE, message = "{validation.inquiryType.invalid}")
    private String inquiryType = "product";

    @NotBlank(message = "{validation.orderPhase.required}")
    @Pattern(regexp = ValidationPatterns.BLANK_OR_ORDER_PHASE, message = "{validation.orderPhase.invalid}")
    private String orderPhase = "before_order";

    @Size(max = 255, message = "{validation.productName.max}")
    private String productName;

    @Size(max = 32, message = "{validation.productCode.max}")
    private String productCode;

    @NotBlank(message = "{validation.contactMessage.required}")
    @Size(max = 1000, message = "{validation.contactMessage.max}")
    private String message;

    /**
     * バリデーションと保存の前に、入力文字列を trim し空欄を null に寄せた複製を返す。
     */
    public ContactForm normalize() {
        ContactForm normalized = new ContactForm();
        normalized.companyName = trimToNull(companyName);
        normalized.departmentName = trimToNull(departmentName);
        normalized.lastName = trimToNull(lastName);
        normalized.firstName = trimToNull(firstName);
        normalized.email = trimToNull(email);
        normalized.phone = trimToNull(phone);
        normalized.inquiryType = trimToNull(inquiryType);
        normalized.orderPhase = trimToNull(orderPhase);
        normalized.productName = trimToNull(productName);
        normalized.productCode = trimToNull(productCode);
        normalized.message = trimToNull(message);
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInquiryType() {
        return inquiryType;
    }

    public void setInquiryType(String inquiryType) {
        this.inquiryType = inquiryType;
    }

    public String getOrderPhase() {
        return orderPhase;
    }

    public void setOrderPhase(String orderPhase) {
        this.orderPhase = orderPhase;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
