package jp.co.skig.officeorder.service.member;

import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.web.auth.MemberPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 会員認証情報を Spring Security の {@link UserDetails} に変換するサービス。
 *
 * <p>
 * ログイン時にメールアドレスから有効会員を検索し、
 * セッションに載せる principal を生成する。
 */
@Service
public class MemberUserDetailsService implements UserDetailsService {

    /** 会員認証情報の取得を担当するサービス。 */
    private final MemberService memberService;

    /**
     * 会員向け UserDetailsService を生成する。
     *
     * @param memberService 会員認証情報の取得サービス
     */
    public MemberUserDetailsService(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * メールアドレスからログイン対象会員を読み込む。
     *
     * @param username ログイン時に入力されたメールアドレス
     * @return Spring Security 用 principal
     * @throws UsernameNotFoundException 会員が見つからない場合
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberCredential credential = memberService.findActiveCredentialByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("会員が見つかりません。"));
        return new MemberPrincipal(
                credential.memberId(),
                credential.email(),
                credential.lastName(),
                credential.firstName(),
                credential.passwordHash());
    }
}
