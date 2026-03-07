package jp.co.skig.officeorder.model.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutInputFormTest {

    /**
     * 注文入力フォームの正規化で、余分な空白を除去しつつ選択値は維持されることを確認する。
     */
    @Test
    void normalize_trimsValuesAndKeepsFlags() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate(" corporate ");
        form.setLastName(" 山田 ");
        form.setFirstName(" 太郎 ");
        form.setLastNameKana(" ヤマダ ");
        form.setFirstNameKana(" タロウ ");
        form.setCompanyName(" 会社 ");
        form.setDepartmentName(" 営業 ");
        form.setEmail(" test@example.com ");
        form.setDaytimePhone(" 0312345678 ");
        form.setFax(" 0312349999 ");
        form.setPostalCodePart1(" 123 ");
        form.setPostalCodePart2(" 4567 ");
        form.setPrefecture(" 東京都 ");
        form.setCity(" 千代田区 ");
        form.setAddressLine(" 1-1-1 ");
        form.setDeliveryFloor(" 5 ");
        form.setHasElevator(Boolean.FALSE);
        form.setPaymentMethod(" convenience_store ");
        form.setSelectedAdditionalAddressId(10L);

        CheckoutInputForm normalized = form.normalize();

        assertThat(normalized.getPersonalOrCorporate()).isEqualTo("corporate");
        assertThat(normalized.getLastName()).isEqualTo("山田");
        assertThat(normalized.getCompanyName()).isEqualTo("会社");
        assertThat(normalized.getHasElevator()).isFalse();
        assertThat(normalized.getSelectedAdditionalAddressId()).isEqualTo(10L);
        assertThat(normalized.getPaymentMethod()).isEqualTo("convenience_store");
    }

    /**
     * 注文確認画面で使う表示補助メソッドが、コード値を期待どおりの表示文言へ変換することを確認する。
     */
    @Test
    void displayHelpers_returnExpectedLabels() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate("corporate");
        form.setHasElevator(Boolean.FALSE);
        form.setPaymentMethod("cash_on_delivery");
        form.setPostalCodePart1("123");
        form.setPostalCodePart2("4567");
        form.setDeliveryFloor("5");

        assertThat(form.isCorporate()).isTrue();
        assertThat(form.getPersonalOrCorporateLabel()).isEqualTo("法人");
        assertThat(form.getHasElevatorLabel()).isEqualTo("なし");
        assertThat(form.getPaymentMethodLabel()).isEqualTo("代金引換");
        assertThat(form.getPostalCode()).isEqualTo("1234567");
        assertThat(form.getPostalCodeDisplay()).isEqualTo("123-4567");
        assertThat(form.getDeliveryFloorDisplay()).isEqualTo("5階");
    }

    /**
     * 郵便番号や階数が未完成の場合は、保存値や表示値を無理に組み立てずにフォールバックすることを確認する。
     */
    @Test
    void displayHelpers_returnFallbackWhenPostalCodeOrFloorIsIncomplete() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPostalCodePart1("123");
        form.setPostalCodePart2(" ");
        form.setDeliveryFloor(" ");

        assertThat(form.getPostalCode()).isNull();
        assertThat(form.getPostalCodeDisplay()).isEqualTo("-");
        assertThat(form.getDeliveryFloorDisplay()).isEqualTo("-");
    }
}
