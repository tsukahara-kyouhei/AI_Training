package jp.co.skig.officeorder.web.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security セッションに保持する会員principal。
 */
public class MemberPrincipal implements UserDetails {

    /** 会員ID。 */
    private final long memberId;
    /** ログイン識別子となるメールアドレス。 */
    private final String email;
    /** 姓。 */
    private final String lastName;
    /** 名。 */
    private final String firstName;
    /** ハッシュ化済みパスワード。 */
    private final String passwordHash;

    /**
     * 会員principalを生成する。
     *
     * @param memberId 会員ID
     * @param email メールアドレス
     * @param lastName 姓
     * @param firstName 名
     * @param passwordHash ハッシュ化済みパスワード
     */
    public MemberPrincipal(long memberId,
                           String email,
                           String lastName,
                           String firstName,
                           String passwordHash) {
        this.memberId = memberId;
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
        this.passwordHash = passwordHash;
    }

    /**
     * 会員IDを返す。
     *
     * @return 会員ID
     */
    public long getMemberId() {
        return memberId;
    }

    /**
     * 姓を返す。
     *
     * @return 姓
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * 名を返す。
     *
     * @return 名
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * 画面表示用のフルネームを返す。
     *
     * @return フルネーム
     */
    public String getFullName() {
        return lastName + " " + firstName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
