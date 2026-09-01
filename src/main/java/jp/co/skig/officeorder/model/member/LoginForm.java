package jp.co.skig.officeorder.model.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ログイン画面の入力値を保持するフォームモデル。
 */
public class LoginForm {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.format}")
    @Size(max = 254, message = "{validation.email.max}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(max = 64, message = "{validation.password.login.max}")
    private String password;

    private String redirectPath;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public void setRedirectPath(String redirectPath) {
        this.redirectPath = redirectPath;
    }
}
