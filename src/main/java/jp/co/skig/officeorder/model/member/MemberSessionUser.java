package jp.co.skig.officeorder.model.member;

/**
 * ログインセッションに保持する会員の軽量情報。
 */
public record MemberSessionUser(
        Long memberId,
        String email,
        String lastName,
        String firstName) {
    /**
     * 画面ヘッダやマイページ表示用に氏名を連結する。
     */
    public String fullName() {
        return lastName + " " + firstName;
    }
}
