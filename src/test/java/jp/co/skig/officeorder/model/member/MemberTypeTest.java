package jp.co.skig.officeorder.model.member;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTypeTest {

    /**
     * DBの会員区分文字列では、corporate のみを法人会員として判定することを確認する。
     */
    @Test
    void fromDbValue_returnsCorporateOnlyForCorporateValue() {
        assertThat(MemberType.fromDbValue("corporate")).isEqualTo(MemberType.CORPORATE);
        assertThat(MemberType.fromDbValue("CORPORATE")).isEqualTo(MemberType.CORPORATE);
        assertThat(MemberType.fromDbValue("personal")).isEqualTo(MemberType.PERSONAL);
        assertThat(MemberType.fromDbValue(null)).isEqualTo(MemberType.PERSONAL);
    }
}
