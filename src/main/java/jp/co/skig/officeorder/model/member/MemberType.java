package jp.co.skig.officeorder.model.member;

/**
 * 会員の個人/法人区分。
 */
public enum MemberType {
    PERSONAL,
    CORPORATE;

    /**
     * DB の区分文字列をアプリケーション用の列挙値へ変換する。
     */
    public static MemberType fromDbValue(String dbValue) {
        if ("corporate".equalsIgnoreCase(dbValue)) {
            return CORPORATE;
        }
        return PERSONAL;
    }
}

