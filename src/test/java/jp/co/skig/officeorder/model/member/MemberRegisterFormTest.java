package jp.co.skig.officeorder.model.member;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRegisterFormTest {

    /**
     * 会員登録フォームの正規化で、余分な空白を除去しつつ日付や選択値は維持されることを確認する。
     */
    @Test
    void normalize_trimsValuesAndKeepsFlags() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate(" corporate ");
        form.setLastName(" 山田 ");
        form.setFirstName(" 花子 ");
        form.setLastNameKana(" ヤマダ ");
        form.setFirstNameKana(" ハナコ ");
        form.setCompanyName(" 会社 ");
        form.setDepartmentName(" 総務 ");
        form.setEmail(" member@example.com ");
        form.setGender(" female ");
        form.setAnniversaryDate(LocalDate.of(1990, 1, 2));
        form.setPassword("Passw0rd!");
        form.setNewsletterOptIn(Boolean.FALSE);
        form.setPostalCodePart1(" 123 ");
        form.setPostalCodePart2(" 4567 ");
        form.setPrefecture(" 東京都 ");
        form.setCity(" 千代田区 ");
        form.setAddressLine(" 1-1-1 ");
        form.setDeliveryFloor(" 8 ");
        form.setHasElevator(Boolean.FALSE);
        form.setDaytimePhone(" 0312345678 ");
        form.setFax(" 0312349999 ");

        MemberRegisterForm normalized = form.normalize();

        assertThat(normalized.getPersonalOrCorporate()).isEqualTo("corporate");
        assertThat(normalized.getLastName()).isEqualTo("山田");
        assertThat(normalized.getDepartmentName()).isEqualTo("総務");
        assertThat(normalized.getEmail()).isEqualTo("member@example.com");
        assertThat(normalized.getGender()).isEqualTo("female");
        assertThat(normalized.getAnniversaryDate()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(normalized.getPassword()).isEqualTo("Passw0rd!");
        assertThat(normalized.getNewsletterOptIn()).isFalse();
        assertThat(normalized.getHasElevator()).isFalse();
        assertThat(normalized.getFax()).isEqualTo("0312349999");
    }

    /**
     * 会員登録確認画面で使う表示補助メソッドが、各コード値を期待どおりの表示文言へ変換することを確認する。
     */
    @Test
    void displayHelpers_returnExpectedLabels() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("corporate");
        form.setGender("no_answer");
        form.setNewsletterOptIn(Boolean.FALSE);
        form.setHasElevator(Boolean.TRUE);
        form.setPostalCodePart1("123");
        form.setPostalCodePart2("4567");
        form.setDeliveryFloor("12");

        assertThat(form.isCorporate()).isTrue();
        assertThat(form.getPersonalOrCorporateLabel()).isEqualTo("法人");
        assertThat(form.getGenderLabel()).isEqualTo("無回答");
        assertThat(form.getNewsletterOptInLabel()).isEqualTo("希望しない");
        assertThat(form.getHasElevatorLabel()).isEqualTo("あり");
        assertThat(form.getPostalCode()).isEqualTo("1234567");
        assertThat(form.getPostalCodeDisplay()).isEqualTo("123-4567");
        assertThat(form.getDeliveryFloorDisplay()).isEqualTo("12階");
    }

    /**
     * 郵便番号や階数が未完成の場合は、保存値や表示値を無理に組み立てずにフォールバックすることを確認する。
     */
    @Test
    void displayHelpers_returnFallbackWhenPostalCodeOrFloorIsIncomplete() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPostalCodePart1("123");
        form.setPostalCodePart2(" ");
        form.setDeliveryFloor(" ");

        assertThat(form.getPostalCode()).isNull();
        assertThat(form.getPostalCodeDisplay()).isEqualTo("-");
        assertThat(form.getDeliveryFloorDisplay()).isEqualTo("-");
    }
}
