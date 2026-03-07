package jp.co.skig.officeorder.testutil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.service.member.MemberService;

/**
 * 会員系テストで使い回す入力フォームと表示モデルのフィクスチャ集。
 *
 * <p>会員登録、会員セッション、追加お届け先、購入履歴の典型的なサンプルを提供し、
 * テストごとの本題でない初期化コードを減らす用途で使う。
 */
public final class MemberTestFixtures {

    private MemberTestFixtures() {
    }

    /**
     * マイページや購入フローで共通利用する標準会員を返す。
     *
     * @return 標準会員セッション情報
     */
    public static MemberSessionUser memberSessionUser() {
        return memberSessionUser(5L, "member@example.com", "山田", "花子");
    }

    /**
     * 任意値で会員セッション情報を生成する。
     *
     * @param memberId 会員ID
     * @param email メールアドレス
     * @param lastName 姓
     * @param firstName 名
     * @return 会員セッション情報
     */
    public static MemberSessionUser memberSessionUser(long memberId,
                                                      String email,
                                                      String lastName,
                                                      String firstName) {
        return new MemberSessionUser(memberId, email, lastName, firstName);
    }

    /**
     * 会員登録フォームの標準入力例を返す。
     *
     * @return 会員登録フォーム
     */
    public static MemberRegisterForm memberRegisterForm() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName(" 山田 ");
        form.setFirstName(" 花子 ");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("ハナコ");
        form.setCompanyName(" ");
        form.setDepartmentName("営業部");
        form.setEmail(" member@example.com ");
        form.setGender("female");
        form.setAnniversaryDate(LocalDate.of(1990, 1, 1));
        form.setPassword("Password!1");
        form.setNewsletterOptIn(Boolean.TRUE);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("1-1-1");
        form.setDeliveryFloor("5");
        form.setHasElevator(Boolean.TRUE);
        form.setDaytimePhone("0312345678");
        form.setFax("0312345679");
        return form;
    }

    /**
     * 更新系テスト向けに、余白を含む会員情報変更フォームを返す。
     *
     * @return 会員情報変更フォーム
     */
    public static MemberProfileEditForm memberProfileEditForm() {
        MemberProfileEditForm form = new MemberProfileEditForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName(" 山田 ");
        form.setFirstName(" 花子 ");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("ハナコ");
        form.setCompanyName(" ");
        form.setDepartmentName("営業部");
        form.setEmail(" updated@example.com ");
        form.setGender("female");
        form.setAnniversaryDate(LocalDate.of(1990, 1, 1));
        form.setNewsletterOptIn(Boolean.TRUE);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("1-1-1");
        form.setDeliveryFloor("5");
        form.setHasElevator(Boolean.TRUE);
        form.setDaytimePhone("0312345678");
        form.setFax("0312345679");
        return form;
    }

    /**
     * 追加お届け先フォームの標準入力例を返す。
     *
     * @return 追加お届け先フォーム
     */
    public static MemberAdditionalAddressForm memberAdditionalAddressForm() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName("山田");
        form.setFirstName("花子");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("ハナコ");
        form.setCompanyName(null);
        form.setDepartmentName("営業部");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("1-1-1");
        form.setDeliveryFloor("5");
        form.setHasElevator(Boolean.TRUE);
        form.setDaytimePhone("0312345678");
        form.setFax("0312345679");
        return form;
    }

    /**
     * 注文入力画面の選択肢で使う追加お届け先を1件だけ持つページを返す。
     *
     * @param memberId 会員ID
     * @return 追加お届け先ページ
     */
    public static MemberAdditionalAddressPage memberAdditionalAddressPage(long memberId) {
        return new MemberAdditionalAddressPage(
                List.of(new MemberAdditionalAddressView(
                        10L,
                        memberId,
                        "山田",
                        "花子",
                        "ヤマダ",
                        "ハナコ",
                        null,
                        "営業部",
                        "1000001",
                        "東京都",
                        "千代田区",
                        "1-1-1",
                        5,
                        true,
                        "0312345678",
                        null,
                        null
                )),
                1,
                1,
                MemberService.MYPAGE_PAGE_SIZE
        );
    }

    /**
     * 購入履歴詳細の標準表示モデルを返す。
     *
     * @return 注文詳細表示モデル
     */
    public static MemberOrderDetailView memberOrderDetailView() {
        return new MemberOrderDetailView(
                "20260307000001",
                OffsetDateTime.parse("2026-03-07T10:00:00+09:00"),
                "completed",
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(3000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(1300),
                BigDecimal.valueOf(14300),
                List.of(),
                List.of()
        );
    }
}
