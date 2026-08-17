package jp.co.skig.officeorder.service.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartCookieStore cartCookieStore;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MessageSource messageSource;

    private CartService cartService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartCookieStore, cartRepository, messageSource);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("getCartのテスト")
    class GetCartTest {

        @Test
        @DisplayName("カート内商品と計算結果（税・送料込）が正しく組み立てられること")
        void shouldReturnCartViewCorrectly() {
            // モック準備
            CartCookieItem cookieItem = new CartCookieItem(10L, 2, true);
            when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));

            CartProductSnapshot snapshot = new CartProductSnapshot(
                    10L, 1L, "オフィスチェア", "CHAIR-01", "ブラック",
                    new BigDecimal("2000"), 10, true, new BigDecimal("500")
            );
            when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                    .thenReturn(Map.of(10L, snapshot));
            when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

            // 実行
            CartView actual = cartService.getCart(request, response);

            // 検証
            assertThat(actual).isNotNull();
            assertThat(actual.items().size()).isEqualTo(1);
            assertThat(actual.totalQuantity()).isEqualTo(2);

            // 商品小計: 2000 * 2 = 4000, 組立費: 500 * 2 = 1000 => 課税対象 5000
            // 消費税: 5000 * 10% = 500
            // 税込商品+組立金額: 5500 >= 5000 (送料無料) => 送料 0
            // 合計: 5000 + 500 + 0 = 5500
            assertThat(actual.summary().productSubtotal()).isEqualByComparingTo("4000");
            assertThat(actual.summary().assemblyFeeTotal()).isEqualByComparingTo("1000");
            assertThat(actual.summary().taxAmount()).isEqualByComparingTo("500");
            assertThat(actual.summary().shippingFee()).isEqualByComparingTo("0");
            assertThat(actual.summary().totalAmount()).isEqualByComparingTo("5500");
        }
    }

    @Nested
    @DisplayName("addItemのテスト")
    class AddItemTest {

        @Test
        @DisplayName("正常なパラメータでカートへ新規追加できること")
        void shouldAddItemSuccessfully() {
            when(cartCookieStore.load(request)).thenReturn(List.of());
            CartProductSnapshot snapshot = new CartProductSnapshot(
                    10L, 1L, "デスク", "DESK-01", "ホワイト",
                    new BigDecimal("10000"), 5, false, BigDecimal.ZERO
            );
            when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                    .thenReturn(Map.of(10L, snapshot));

            cartService.addItem(request, response, 10L, 1, false);

            verify(cartCookieStore).save(eq(request), eq(response), any());
        }

        @Test
        @DisplayName("不正な数量を指定した場合に例外が発生すること")
        void shouldThrowExceptionWhenQuantityInvalid() {
            when(messageSource.getMessage(eq("business.cart.quantityRange"), any(), any()))
                    .thenReturn("数量が不正です");

            assertThatThrownBy(() -> cartService.addItem(request, response, 10L, 0, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("数量が不正です");

            verify(cartCookieStore, never()).save(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("sanitizeRedirectPathのテスト")
    class SanitizeRedirectPathTest {

        @Test
        @DisplayName("安全な相対パスのみが許可されること")
        void shouldSanitizeRedirectPath() {
            assertThat(cartService.sanitizeRedirectPath("/cart")).isEqualTo("/cart");
            assertThat(cartService.sanitizeRedirectPath("https://example.com")).isNull();
            assertThat(cartService.sanitizeRedirectPath("//example.com")).isNull();
            assertThat(cartService.sanitizeRedirectPath("")).isNull();
            assertThat(cartService.sanitizeRedirectPath(null)).isNull();
        }
    }
}
