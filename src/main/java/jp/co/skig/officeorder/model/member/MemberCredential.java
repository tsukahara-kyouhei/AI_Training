package jp.co.skig.officeorder.model.member;

/**
 * 認証処理に必要な会員資格情報。
 */
public record MemberCredential(
        Long memberId,
        String email,
        String lastName,
        String firstName,
        String passwordHash
) {
    /**
     * 認証済み会員をセッション保持用の軽量モデルへ変換する。
     */
    public MemberSessionUser toSessionUser() {
        return new MemberSessionUser(memberId, email, lastName, firstName);
    }
}
