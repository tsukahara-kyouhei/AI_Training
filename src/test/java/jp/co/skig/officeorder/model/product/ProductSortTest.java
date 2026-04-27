package jp.co.skig.officeorder.model.product;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProductSort} の単体テスト。
 */
class ProductSortTest {

    @Nested
    class FromValueTest {

        @Test
        void nullはdefaultValueを返す() {
            assertThat(ProductSort.fromValue(null, ProductSort.RECOMMENDED)).isEqualTo(ProductSort.RECOMMENDED);
        }

        @Test
        void 空文字はdefaultValueを返す() {
            assertThat(ProductSort.fromValue("", ProductSort.NEWEST)).isEqualTo(ProductSort.NEWEST);
        }

        @Test
        void 空白のみはdefaultValueを返す() {
            assertThat(ProductSort.fromValue("   ", ProductSort.RECOMMENDED)).isEqualTo(ProductSort.RECOMMENDED);
        }

        @Test
        void 不正な値はdefaultValueを返す() {
            assertThat(ProductSort.fromValue("invalid", ProductSort.PRICE_ASC)).isEqualTo(ProductSort.PRICE_ASC);
        }

        @Test
        void recommended文字列はRECOMMENDEDを返す() {
            assertThat(ProductSort.fromValue("recommended", ProductSort.NEWEST)).isEqualTo(ProductSort.RECOMMENDED);
        }

        @Test
        void newest文字列はNEWESTを返す() {
            assertThat(ProductSort.fromValue("newest", ProductSort.RECOMMENDED)).isEqualTo(ProductSort.NEWEST);
        }

        @Test
        void price_asc文字列はPRICE_ASCを返す() {
            assertThat(ProductSort.fromValue("price_asc", ProductSort.RECOMMENDED)).isEqualTo(ProductSort.PRICE_ASC);
        }

        @Test
        void price_desc文字列はPRICE_DESCを返す() {
            assertThat(ProductSort.fromValue("price_desc", ProductSort.RECOMMENDED)).isEqualTo(ProductSort.PRICE_DESC);
        }

        @Test
        void 大文字でも一致する() {
            assertThat(ProductSort.fromValue("RECOMMENDED", ProductSort.NEWEST)).isEqualTo(ProductSort.RECOMMENDED);
        }

        @Test
        void 大文字小文字混在でも一致する() {
            assertThat(ProductSort.fromValue("Price_Asc", ProductSort.RECOMMENDED)).isEqualTo(ProductSort.PRICE_ASC);
        }
    }
}
