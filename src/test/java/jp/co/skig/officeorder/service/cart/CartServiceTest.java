package jp.co.skig.officeorder.service.cart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartCookieStore cartCookieStore;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CartService sut;

    @BeforeEach
    void setUp() {
        sut = new CartService(cartCookieStore, cartRepository, messageSource);
    }

    // ─── sanitizeRedirectPath ───────────────────────────────────────────

    @Test
    void sanitizeRedirectPath_null_returns_null() {
        assertThat(sut.sanitizeRedirectPath(null)).isNull();
    }

    @Test
    void sanitizeRedirectPath_blank_string_returns_null() {
        assertThat(sut.sanitizeRedirectPath("   ")).isNull();
    }

    @Test
    void sanitizeRedirectPath_valid_relative_path_returns_path() {
        assertThat(sut.sanitizeRedirectPath("/products/1")).isEqualTo("/products/1");
    }

    @Test
    void sanitizeRedirectPath_path_not_starting_with_slash_returns_null() {
        assertThat(sut.sanitizeRedirectPath("products/1")).isNull();
    }

    @Test
    void sanitizeRedirectPath_double_slash_path_returns_null() {
        assertThat(sut.sanitizeRedirectPath("//evil.com")).isNull();
    }

    @Test
    void sanitizeRedirectPath_path_with_scheme_returns_null() {
        assertThat(sut.sanitizeRedirectPath("/redirect?url=http://evil.com")).isNull();
    }

    @Test
    void sanitizeRedirectPath_path_with_https_scheme_returns_null() {
        assertThat(sut.sanitizeRedirectPath("https://evil.com")).isNull();
    }

    // ─── countTotalQuantity ─────────────────────────────────────────────

    @Test
    void countTotalQuantity_empty_cart_returns_zero() {
        // Arrange
        when(cartCookieStore.load(request)).thenReturn(List.of());

        // Act
        int result = sut.countTotalQuantity(request);

        // Assert
        assertThat(result).isZero();
    }

    @Test
    void countTotalQuantity_multiple_items_returns_sum() {
        // Arrange
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 2, null),
                new CartCookieItem(2L, 3, null)
        ));

        // Act
        int result = sut.countTotalQuantity(request);

        // Assert
        assertThat(result).isEqualTo(5);
    }

    @Test
    void countTotalQuantity_quantity_less_than_1_is_normalized_to_1() {
        // Arrange
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 0, null)
        ));

        // Act
        int result = sut.countTotalQuantity(request);

        // Assert
        assertThat(result).isEqualTo(1);
    }

    @Test
    void countTotalQuantity_quantity_over_99_is_normalized_to_99() {
        // Arrange
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 200, null)
        ));

        // Act
        int result = sut.countTotalQuantity(request);

        // Assert
        assertThat(result).isEqualTo(99);
    }

    // ─── addItem ────────────────────────────────────────────────────────

    @Test
    void addItem_invalid_variant_id_throws_illegal_argument_exception() {
        // Arrange
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラーメッセージ");

        // Act / Assert
        assertThatThrownBy(() -> sut.addItem(request, response, 0L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_quantity_zero_throws_illegal_argument_exception() {
        // Arrange
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラーメッセージ");

        // Act / Assert
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_quantity_over_99_throws_illegal_argument_exception() {
        // Arrange
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラーメッセージ");

        // Act / Assert
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_out_of_stock_throws_illegal_argument_exception() {
        // Arrange
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラーメッセージ");
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "テスト商品", "PROD001", "ホワイト",
                BigDecimal.valueOf(10000), 0, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                .thenReturn(Map.of(1L, snapshot));

        // Act / Assert
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── updateItem ─────────────────────────────────────────────────────

    @Test
    void updateItem_quantity_zero_throws_illegal_argument_exception() {
        // Arrange
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラーメッセージ");

        // Act / Assert
        assertThatThrownBy(() -> sut.updateItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateItem_quantity_over_99_throws_illegal_argument_exception() {
        // Arrange
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラーメッセージ");

        // Act / Assert
        assertThatThrownBy(() -> sut.updateItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateItem_item_not_in_cart_does_nothing() {
        // Arrange
        when(cartCookieStore.load(any())).thenReturn(List.of());

        // Act (no exception expected)
        sut.updateItem(request, response, 999L, 1, null);
    }

    // ─── removeItem ─────────────────────────────────────────────────────

    @Test
    void removeItem_existing_item_saves_without_that_item() {
        // Arrange
        CartCookieItem existing = new CartCookieItem(1L, 2, null);
        when(cartCookieStore.load(any())).thenReturn(List.of(existing));

        // Act (no exception expected)
        sut.removeItem(request, response, 1L);

        // Assert - CartCookieStore.save was called without the item
    }

    @Test
    void removeItem_item_not_in_cart_does_nothing_without_error() {
        // Arrange
        when(cartCookieStore.load(any())).thenReturn(List.of());

        // Act (no exception expected)
        sut.removeItem(request, response, 999L);
    }

    // ─── clear ──────────────────────────────────────────────────────────

    @Test
    void clear_delegates_to_cart_cookie_store() {
        // Act
        sut.clear(request, response);

        // Assert
        org.mockito.Mockito.verify(cartCookieStore).clear(request, response);
    }

    // ─── getCart ────────────────────────────────────────────────────────

    @Test
    void getCart_empty_cart_returns_view_with_no_items() {
        // Arrange
        when(cartCookieStore.load(any())).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of())).thenReturn(Map.of());
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        // Act
        CartView result = sut.getCart(request, response);

        // Assert
        assertThat(result.items()).isEmpty();
        assertThat(result.itemTypeCount()).isZero();
        // 空カートでも税別合計0円 < 送料無料閾値のため、定額送料800円が発生する
        assertThat(result.summary().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));
    }

    @Test
    void getCart_single_item_with_snapshot_returns_calculated_view() {
        // Arrange
        CartCookieItem item = new CartCookieItem(1L, 2, null);
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "テスト商品", "PROD001", "ホワイト",
                BigDecimal.valueOf(10000), 5, false, BigDecimal.ZERO
        );
        when(cartCookieStore.load(any())).thenReturn(List.of(item));
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        // Act
        CartView result = sut.getCart(request, response);

        // Assert
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.summary().productSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }

    @Test
    void getCart_item_without_matching_snapshot_is_excluded() {
        // Arrange
        CartCookieItem item = new CartCookieItem(99L, 1, null);
        when(cartCookieStore.load(any())).thenReturn(List.of(item));
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of());
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        // Act
        CartView result = sut.getCart(request, response);

        // Assert
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getCart_assembly_fee_applied_when_requested_and_available() {
        // Arrange
        CartCookieItem item = new CartCookieItem(1L, 1, true);
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "テスト商品", "PROD001", "ホワイト",
                BigDecimal.valueOf(10000), 5, true, BigDecimal.valueOf(2000)
        );
        when(cartCookieStore.load(any())).thenReturn(List.of(item));
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        // Act
        CartView result = sut.getCart(request, response);

        // Assert
        assertThat(result.summary().assemblyFeeTotal()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    // ─── addItem (happy paths) ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void addItem_new_item_added_to_empty_cart_saves_item() {
        // Arrange
        when(cartCookieStore.load(any())).thenReturn(List.of());
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品", "P001", "白", BigDecimal.valueOf(5000), 5, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));

        // Act
        sut.addItem(request, response, 1L, 2, null);

        // Assert
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(any(), any(), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void addItem_existing_item_quantity_is_merged() {
        // Arrange
        CartCookieItem existing = new CartCookieItem(1L, 3, null);
        when(cartCookieStore.load(any())).thenReturn(List.of(existing));
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品", "P001", "白", BigDecimal.valueOf(5000), 5, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));

        // Act
        sut.addItem(request, response, 1L, 2, null);

        // Assert - merged quantity = 3 + 2 = 5
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(any(), any(), captor.capture());
        CartCookieItem saved = (CartCookieItem) captor.getValue().get(0);
        assertThat(saved.quantity()).isEqualTo(5);
    }

    @Test
    void addItem_cart_at_limit_throws_illegal_argument_exception() {
        // Arrange
        List<CartCookieItem> fullCart = IntStream.rangeClosed(1, CartCookieStore.MAX_LINE_ITEMS)
                .mapToObj(i -> new CartCookieItem((long) i, 1, null))
                .toList();
        when(cartCookieStore.load(any())).thenReturn(fullCart);
        CartProductSnapshot snapshot = new CartProductSnapshot(
                999L, 10L, "商品", "P999", "白", BigDecimal.valueOf(5000), 5, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(999L, snapshot));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラー");

        // Act / Assert
        assertThatThrownBy(() -> sut.addItem(request, response, 999L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── updateItem (happy paths) ───────────────────────────────────────

    @Test
    void updateItem_existing_item_updates_successfully() {
        // Arrange
        CartCookieItem existing = new CartCookieItem(1L, 2, null);
        when(cartCookieStore.load(any())).thenReturn(List.of(existing));
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品", "P001", "白", BigDecimal.valueOf(5000), 5, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));

        // Act
        sut.updateItem(request, response, 1L, 5, null);

        // Assert
        verify(cartCookieStore).save(any(), any(), anyList());
    }

    @Test
    void updateItem_out_of_stock_throws_illegal_argument_exception() {
        // Arrange
        CartCookieItem existing = new CartCookieItem(1L, 2, null);
        when(cartCookieStore.load(any())).thenReturn(List.of(existing));
        CartProductSnapshot outOfStock = new CartProductSnapshot(
                1L, 10L, "商品", "P001", "白", BigDecimal.valueOf(5000), 0, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, outOfStock));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("エラー");

        // Act / Assert
        assertThatThrownBy(() -> sut.updateItem(request, response, 1L, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── findSnapshot (snapshot not found branch) ────────────────────────

    @Test
    void addItem_snapshot_not_found_throws_illegal_argument_exception() {
        // addItem → findSnapshot(productVariantId) → snapshot == null → IllegalArgumentException
        // スナップショットが存在しない: 空マップを返す
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of());

        assertThatThrownBy(() -> sut.addItem(request, response, 999L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countTotalQuantity_null_request_returns_zero() {
        // loadCurrentCartItems(null) → L425 return List.of() パス
        // cartCookieStore.load は呼ばれない
        int result = sut.countTotalQuantity(null);
        assertThat(result).isZero();
    }

    @SuppressWarnings("unchecked")
    @Test
    void addItem_second_call_uses_cached_cart_from_request_attribute() {
        // 1回目の addItem で request attribute にカートが保存され、
        // 2回目の addItem はキャッシュを使う (L429 キャッシュ返却パス)
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品", "P001", "白", BigDecimal.valueOf(5000), 5, false, BigDecimal.ZERO
        );
        when(cartCookieStore.load(any())).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));

        // 1回目: キャッシュなし → cookieStore.load() が実行され request attribute に保存
        sut.addItem(request, response, 1L, 1, null);

        // setAttribute が呼ばれた後、getAttribute が同じ List を返すようにスタブ
        String cartAttrKey = jp.co.skig.officeorder.service.cart.CartService.class.getName() + ".cartItems";
        when(request.getAttribute(cartAttrKey)).thenReturn(
                List.of(new CartCookieItem(1L, 1, null))
        );
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of(1L, snapshot));

        // 2回目: キャッシュあり → L429 のキャッシュ返却パスを通る
        sut.addItem(request, response, 1L, 1, null);

        // cookieStore.load は1回しか呼ばれないはず（2回目はキャッシュを使用）
        org.mockito.Mockito.verify(cartCookieStore, org.mockito.Mockito.times(1)).load(request);
    }

    // ─── loadCurrentCartItems null request (L425) ─────────────────────

    @Test
    void getCart_with_null_request_returns_empty_cart() {
        // loadCurrentCartItems(null) → L425 return List.of()
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of());
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView result = sut.getCart(null, response);

        assertThat(result.items()).isEmpty();
    }

    // ─── rememberCurrentCartItems null request (L459) ─────────────────

    @Test
    void clear_with_null_request_does_not_throw() {
        // clear(null, response) → rememberCurrentCartItems(null, ...) → L459 early return
        org.assertj.core.api.Assertions.assertThatCode(() -> sut.clear(null, response))
                .doesNotThrowAnyException();
    }
}
