package jp.co.skig.officeorder.model.member;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberPageModelTest {

    /**
     * 会員向け一覧ページの総ページ数計算が、0件時の下限1ページと端数切り上げの両方を満たすことを確認する。
     */
    @Test
    void totalPages_roundsUpAndReturnsOneForZeroCount() {
        assertThat(new MemberOrderHistoryPage(List.of(), 0, 1, 10).totalPages()).isEqualTo(1);
        assertThat(new MemberOrderHistoryPage(List.of(), 21, 1, 10).totalPages()).isEqualTo(3);
        assertThat(new MemberFavoritePage(List.of(), 0, 1, 8).totalPages()).isEqualTo(1);
        assertThat(new MemberFavoritePage(List.of(), 17, 1, 8).totalPages()).isEqualTo(3);
        assertThat(new MemberAdditionalAddressPage(List.of(), 11, 1, 5).totalPages()).isEqualTo(3);
    }

    /**
     * 注文詳細表示モデルが、日時・ステータス・金額を画面表示用の文言へ整形できることを確認する。
     */
    @Test
    void memberOrderDetailView_formatsStatusDateAndAmountsForDisplay() {
        MemberOrderDetailView detail = new MemberOrderDetailView(
                "202603070001",
                OffsetDateTime.parse("2026-03-07T12:34:00+09:00"),
                "processing",
                new BigDecimal("120000"),
                new BigDecimal("6000"),
                new BigDecimal("800"),
                new BigDecimal("12680"),
                new BigDecimal("139480"),
                List.of(),
                List.of()
        );

        assertThat(detail.orderDatetimeDisplay()).isEqualTo("2026-03-07 12:34");
        assertThat(detail.orderStatusLabel()).isEqualTo("処理中");
        assertThat(detail.subtotalAmountText()).isEqualTo("120,000");
        assertThat(detail.assemblyFeeTotalText()).isEqualTo("6,000");
        assertThat(detail.shippingFeeText()).isEqualTo("800");
        assertThat(detail.taxAmountText()).isEqualTo("12,680");
        assertThat(detail.totalAmountText()).isEqualTo("139,480");
    }
}
