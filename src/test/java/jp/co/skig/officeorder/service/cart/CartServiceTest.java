package jp.co.skig.officeorder.service.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartCookieStore cartCookieStore;

    @Mock
    CartRepository cartRepository;

    @Mock
    MessageSource messageSource;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        cartService = new CartService(cartCookieStore, cartRepository, messageSource);
        // デフォルトでキャッシュなし（リクエスト属性はnull）
        lenient().when(request.getAttribute(anyString())).thenReturn(null);
    }

    // --- getCart ---

    @Test
    void getCart_emptyCart_returnsEmptyCartView() {
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of()))
                .thenReturn(Map.of());
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView result = cartService.getCart(request, response);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.totalQuantity()).isEqualTo(0);
    }

    @Test
    void getCart_singleItemWithFreeShipping_calculatesCorrectTotals() {
        CartCookieItem cookieItem = new CartCookieItem(1L, 2, null);
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "テストデスク", "CODE-001", "白",
                new BigDecimal("30000"), 10, false, BigDecimal.ZERO);

        when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView result = cartService.getCart(request, response);

        // productSubtotal = 30000 * 2 = 60000, taxAmount = floor(60000 * 10 / 100) = 6000
        // shippingTarget = 66000 >= 5000 → shippingFee = 0
        // totalAmount = 60000 + 6000 + 0 = 66000
        assertThat(result.totalQuantity()).isEqualTo(2);
        assertThat(result.summary().productSubtotal()).isEqualByComparingTo("60000");
        assertThat(result.summary().taxAmount()).isEqualByComparingTo("6000");
        assertThat(result.summary().shippingFee()).isEqualByComparingTo("0");
        assertThat(result.summary().totalAmount()).isEqualByComparingTo("66000");
    }

    @Test
    void getCart_lowPriceItem_appliesFlatShippingFee() {
        CartCookieItem cookieItem = new CartCookieItem(1L, 1, null);
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "格安アクセサリー", "ACC-001", "黒",
                new BigDecimal("100"), 5, false, BigDecimal.ZERO);

        when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView result = cartService.getCart(request, response);

        // productSubtotal = 100, taxAmount = 10, shippingTarget = 110 < 5000 → shippingFee = 800
        assertThat(result.summary().shippingFee()).isEqualByComparingTo("800");
        assertThat(result.summary().totalAmount()).isEqualByComparingTo("910");
    }

    @Test
    void getCart_assemblyItem_includesAssemblyFeeInTotal() {
        CartCookieItem cookieItem = new CartCookieItem(1L, 1, true);
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "組立デスク", "DESK-001", "白",
                new BigDecimal("50000"), 5, true, new BigDecimal("5000"));

        when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView result = cartService.getCart(request, response);

        // productSubtotal = 50000, assemblyFeeTotal = 5000
        // taxableSubtotal = 55000, taxAmount = 5500
        assertThat(result.summary().assemblyFeeTotal()).isEqualByComparingTo("5000");
        assertThat(result.summary().taxAmount()).isEqualByComparingTo("5500");
    }

    @Test
    void getCart_unknownVariantId_skipsItem() {
        CartCookieItem cookieItem = new CartCookieItem(999L, 1, null);
        when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(999L)))
                .thenReturn(Map.of()); // スナップショットなし
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView result = cartService.getCart(request, response);

        assertThat(result.isEmpty()).isTrue();
    }

    // --- countTotalQuantity ---

    @Test
    void countTotalQuantity_multipleItems_returnsSumOfQuantities() {
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 3, null),
                new CartCookieItem(2L, 2, null)));

        int total = cartService.countTotalQuantity(request);

        assertThat(total).isEqualTo(5);
    }

    @Test
    void countTotalQuantity_zeroQuantityItem_normalizesToOne() {
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 0, null)));

        int total = cartService.countTotalQuantity(request);

        assertThat(total).isEqualTo(1);
    }

    // --- addItem ---

    @Test
    void addItem_invalidProductVariantId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> cartService.addItem(request, response, 0L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartCookieStore, never()).save(any(), any(), any());
    }

    @Test
    void addItem_quantityTooLow_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> cartService.addItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_quantityTooHigh_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> cartService.addItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_outOfStock_throwsIllegalArgumentException() {
        CartProductSnapshot outOfStockSnapshot = new CartProductSnapshot(
                1L, 101L, "在庫なし商品", "CODE-X", "黒",
                new BigDecimal("10000"), 0, false, BigDecimal.ZERO);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, outOfStockSnapshot));

        assertThatThrownBy(() -> cartService.addItem(request, response, 1L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItem_newItem_addsToCart() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "テスト商品", "CODE-001", "白",
                new BigDecimal("10000"), 5, false, BigDecimal.ZERO);
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        cartService.addItem(request, response, 1L, 2, null);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.captor();
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).productVariantId()).isEqualTo(1L);
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void addItem_existingItem_mergesQuantity() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "テスト商品", "CODE-001", "白",
                new BigDecimal("10000"), 10, false, BigDecimal.ZERO);
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 3, null)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        cartService.addItem(request, response, 1L, 2, null);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.captor();
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(5); // 3 + 2
    }

    @Test
    void addItem_existingItemMergeExceedsMax_capsAtNinetyNine() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "テスト商品", "CODE-001", "白",
                new BigDecimal("10000"), 99, false, BigDecimal.ZERO);
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 50, null)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        cartService.addItem(request, response, 1L, 60, null);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.captor();
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(99); // cap at 99
    }

    @Test
    void addItem_cartAtMaxLineItems_throwsIllegalArgumentException() {
        List<CartCookieItem> fullCart = new ArrayList<>();
        for (int i = 1; i <= CartCookieStore.MAX_LINE_ITEMS; i++) {
            fullCart.add(new CartCookieItem(i, 1, null));
        }
        CartProductSnapshot snapshot = new CartProductSnapshot(
                999L, 999L, "新商品", "NEW-001", "白",
                new BigDecimal("5000"), 10, false, BigDecimal.ZERO);
        when(cartCookieStore.load(request)).thenReturn(fullCart);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(999L)))
                .thenReturn(Map.of(999L, snapshot));

        assertThatThrownBy(() -> cartService.addItem(request, response, 999L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- updateItem ---

    @Test
    void updateItem_quantityOutOfRange_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> cartService.updateItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> cartService.updateItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateItem_itemNotInCart_doesNothing() {
        when(cartCookieStore.load(request)).thenReturn(List.of());

        cartService.updateItem(request, response, 1L, 3, null);

        verify(cartCookieStore, never()).save(any(), any(), any());
    }

    @Test
    void updateItem_outOfStockItem_throwsIllegalArgumentException() {
        CartProductSnapshot outOfStock = new CartProductSnapshot(
                1L, 101L, "在庫切れ商品", "CODE-X", "白",
                new BigDecimal("10000"), 0, false, BigDecimal.ZERO);
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 1, null)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, outOfStock));

        assertThatThrownBy(() -> cartService.updateItem(request, response, 1L, 2, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateItem_validItem_updatesQuantity() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 101L, "テスト商品", "CODE-001", "白",
                new BigDecimal("10000"), 5, false, BigDecimal.ZERO);
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 1, null)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        cartService.updateItem(request, response, 1L, 3, null);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.captor();
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(3);
    }

    // --- removeItem ---

    @Test
    void removeItem_existingItem_removesFromCart() {
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 1, null),
                new CartCookieItem(2L, 2, null)));

        cartService.removeItem(request, response, 1L);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.captor();
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).productVariantId()).isEqualTo(2L);
    }

    @Test
    void removeItem_nonExistingItem_cartUnchanged() {
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 1, null)));

        cartService.removeItem(request, response, 999L);

        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.captor();
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    // --- clear ---

    @Test
    void clear_delegatesToCookieStore() {
        cartService.clear(request, response);

        verify(cartCookieStore).clear(request, response);
    }

    // --- sanitizeRedirectPath ---

    @Test
    void sanitizeRedirectPath_null_returnsNull() {
        assertThat(cartService.sanitizeRedirectPath(null)).isNull();
    }

    @Test
    void sanitizeRedirectPath_blank_returnsNull() {
        assertThat(cartService.sanitizeRedirectPath("   ")).isNull();
    }

    @Test
    void sanitizeRedirectPath_doesNotStartWithSlash_returnsNull() {
        assertThat(cartService.sanitizeRedirectPath("relative/path")).isNull();
    }

    @Test
    void sanitizeRedirectPath_startsWithDoubleSlash_returnsNull() {
        assertThat(cartService.sanitizeRedirectPath("//evil.com/path")).isNull();
    }

    @Test
    void sanitizeRedirectPath_containsScheme_returnsNull() {
        assertThat(cartService.sanitizeRedirectPath("http://evil.com/path")).isNull();
    }

    @Test
    void sanitizeRedirectPath_validRelativePath_returnsPath() {
        assertThat(cartService.sanitizeRedirectPath("/products/101")).isEqualTo("/products/101");
    }

    @Test
    void sanitizeRedirectPath_validRootPath_returnsRoot() {
        assertThat(cartService.sanitizeRedirectPath("/")).isEqualTo("/");
    }
}
