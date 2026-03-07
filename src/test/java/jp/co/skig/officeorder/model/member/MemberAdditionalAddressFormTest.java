package jp.co.skig.officeorder.model.member;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberAdditionalAddressFormTest {

    /**
     * 追加お届け先フォームの正規化で、文字列の trim とエレベーター有無の既定値補完が行われることを確認する。
     */
    @Test
    void normalize_trimsValuesAndDefaultsElevatorFlag() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName(" 山田 ");
        form.setFirstName(" 太郎 ");
        form.setLastNameKana(" ヤマダ ");
        form.setFirstNameKana(" タロウ ");
        form.setCompanyName(" 会社 ");
        form.setDepartmentName(" 営業 ");
        form.setPostalCodePart1(" 123 ");
        form.setPostalCodePart2(" 4567 ");
        form.setPrefecture(" 東京都 ");
        form.setCity(" 千代田区 ");
        form.setAddressLine(" 1-1-1 ");
        form.setDeliveryFloor(" 5 ");
        form.setHasElevator(null);
        form.setDaytimePhone(" 0312345678 ");
        form.setFax(" 0312349999 ");

        MemberAdditionalAddressForm normalized = form.normalize();

        assertThat(normalized.getLastName()).isEqualTo("山田");
        assertThat(normalized.getCompanyName()).isEqualTo("会社");
        assertThat(normalized.getPostalCode()).isEqualTo("1234567");
        assertThat(normalized.getHasElevator()).isFalse();
        assertThat(normalized.getFax()).isEqualTo("0312349999");
    }

    /**
     * 郵便番号が前後に分割入力されていない場合は、保存用郵便番号を組み立てないことを確認する。
     */
    @Test
    void getPostalCode_returnsNullWhenPostalCodeIsIncomplete() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setPostalCodePart1("123");
        form.setPostalCodePart2(" ");

        assertThat(form.getPostalCode()).isNull();
    }
}
