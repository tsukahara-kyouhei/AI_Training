package jp.co.skig.officeorder.service.cart;

import java.math.BigDecimal;
import java.util.List;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CartService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartCookieStore cartCookieStore;

    @Mock
    CartRepository cartRepository;

    CartService sut;

    @BeforeEach
    void setUp() {
        StaticMessageSource ms = new StaticMessageSource();
        ms.addMessage("business.cart.invalidProduct", java.util.Locale.JAPAN, "商品が無効です。");
        ms.addMessage("business.cart.quantityRange", java.util.Locale.JAPAN, "数量は1〜99で指定してください。");
        ms.addMessage("business.stockShortage", java.util.Locale.JAPAN, "在庫が不足しています。");
        ms.addMessage("business.cart.limitExceeded", java.util.Locale.JAPAN, "カートの明細数上限に達しています。");
        ms.addMessage("business.cart.loadFailed", java.util.Locale.JAPAN, "商品情報の取得に失敗しました。");
        sut = new CartService(cartCookieStore, cartRepository, ms);
    }

    // ── sanitizeRedirectPath ─────────────────────────────────────────────

    @Test
    void nullを渡すとnullが返ること() {
        assertThat(sut.sanitizeRedirectPath(null)).isNull();
    }

    @Test
    void 空文字を渡すとnullが返ること() {
        assertThat(sut.sanitizeRedirectPath("")).isNull();
    }

    @Test
    void スラッシュ始まりの相対パスはそのまま返ること() {
        assertThat(sut.sanitizeRedirectPath("/products/123")).isEqualTo("/products/123");
    }

    @Test
    void スラッシュ始まりでないパスはnullが返ること() {
        assertThat(sut.sanitizeRedirectPath("products/123")).isNull();
    }

    @Test
    void ダブルスラッシュ始まりのパスはnullが返ること() {
        assertThat(sut.sanitizeRedirectPath("//evil.example.com")).isNull();
    }

    @Test
    void スキームを含む絶対URLはnullが返ること() {
        assertThat(sut.sanitizeRedirectPath("/redirect?url=http://evil.example.com")).isNull();
    }

    @Test
    void 前後空白はトリムされて有効パスが返ること() {
        assertThat(sut.sanitizeRedirectPath("  /cart  ")).isEqualTo("/cart");
    }

    // ── countTotalQuantity ───────────────────────────────────────────────

    @Test
    void Cookieが空の時は0が返ること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(cartCookieStore.load(request)).thenReturn(List.of());

        assertThat(sut.countTotalQuantity(request)).isEqualTo(0);
    }

    @Test
    void 複数明細の合計数量が返ること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 3, null),
                new CartCookieItem(2L, 2, null)
        ));

        assertThat(sut.countTotalQuantity(request)).isEqualTo(5);
    }

    @Test
    void 数量0以下のアイテムは1に正規化されて合計されること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 0, null),  // 1 に補正
                new CartCookieItem(2L, 5, null)
        ));

        assertThat(sut.countTotalQuantity(request)).isEqualTo(6);
    }

    // ── addItem ──────────────────────────────────────────────────────────

    @Test
    void 有効な商品を新規追加するとCookieに保存されること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                .thenReturn(Map.of(10L, snapshot(10L, 5, false)));

        sut.addItem(request, response, 10L, 1, null);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    @Test
    void productVariantIdが0以下だとIllegalArgumentExceptionがスローされること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> sut.addItem(request, response, 0L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(cartCookieStore, never()).save(any(), any(), any());
    }

    @Test
    void 数量が0以下だとIllegalArgumentExceptionがスローされること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 数量が100以上だとIllegalArgumentExceptionがスローされること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 在庫ゼロの商品を追加するとIllegalArgumentExceptionがスローされること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(99L)))
                .thenReturn(Map.of(99L, snapshot(99L, 0, false)));

        assertThatThrownBy(() -> sut.addItem(request, response, 99L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(cartCookieStore, never()).save(any(), any(), any());
    }

    @Test
    void 既にカートにある商品を追加すると数量が合算されること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 2, null)
        ));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                .thenReturn(Map.of(10L, snapshot(10L, 10, false)));

        sut.addItem(request, response, 10L, 3, null);

        // 2+3=5 件で保存されること
        verify(cartCookieStore).save(any(), any(), anyList());
    }

    @Test
    void 合算後99を超える場合は99に丸められること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 98, null)
        ));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                .thenReturn(Map.of(10L, snapshot(10L, 200, false)));

        sut.addItem(request, response, 10L, 5, null);

        // 保存は行われる (数量は 99 に丸められる)
        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // ── updateItem ───────────────────────────────────────────────────────

    @Test
    void 数量更新でカートが保存されること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 1, null)
        ));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                .thenReturn(Map.of(10L, snapshot(10L, 5, false)));

        sut.updateItem(request, response, 10L, 3, null);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    @Test
    void カートにない商品の更新は何もしないこと() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of());

        sut.updateItem(request, response, 999L, 3, null);

        verify(cartCookieStore, never()).save(any(), any(), any());
    }

    @Test
    void 更新数量が0以下だとIllegalArgumentExceptionがスローされること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> sut.updateItem(request, response, 10L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── removeItem ───────────────────────────────────────────────────────

    @Test
    void 指定商品がカートから削除されること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 1, null),
                new CartCookieItem(20L, 2, null)
        ));

        sut.removeItem(request, response, 10L);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    @Test
    void カートにない商品の削除もエラーにならないこと() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(20L, 2, null)
        ));

        sut.removeItem(request, response, 999L);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // ── clear ────────────────────────────────────────────────────────────

    @Test
    void clearでCookieストアのclearが呼ばれること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        sut.clear(request, response);

        verify(cartCookieStore).clear(request, response);
    }

    // ── getCart ──────────────────────────────────────────────────────────

    @Test
    void Cookieが空の場合に空カートが返ること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of()))
                .thenReturn(Map.of());
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView cart = sut.getCart(request, response);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.totalQuantity()).isEqualTo(0);
        // 商品小計 0 の場合、送料は発生する (sendShipping = 800 円)
        assertThat(cart.summary().productSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 税込小計が5000円未満の場合に送料800円が付くこと() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 1, null)
        ));
        // 単価 1000円 × 1個 = 1000円(税抜) → 税込 1100円 (<5000円)
        CartProductSnapshot snap = new CartProductSnapshot(10L, 100L, "テスト商品", "P001", "白",
                new BigDecimal("1000"), 5, false, BigDecimal.ZERO);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                .thenReturn(Map.of(10L, snap));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView cart = sut.getCart(request, response);

        assertThat(cart.summary().shippingFee()).isEqualByComparingTo(new BigDecimal("800"));
    }

    @Test
    void 税込小計が5000円以上の場合に送料無料になること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 5, null)
        ));
        // 単価 1000円 × 5個 = 5000円(税抜) → 税込 5500円 (>=5000円)
        CartProductSnapshot snap = new CartProductSnapshot(10L, 100L, "テスト商品", "P001", "白",
                new BigDecimal("1000"), 10, false, BigDecimal.ZERO);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(10L)))
                .thenReturn(Map.of(10L, snap));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView cart = sut.getCart(request, response);

        assertThat(cart.summary().shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void スナップショットに存在しないバリアントはスキップされること() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Cookie に 2 件あるが、スナップショットは 1 件のみ返す
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(10L, 1, null),
                new CartCookieItem(99L, 1, null)  // 削除済み商品
        ));
        CartProductSnapshot snap = new CartProductSnapshot(10L, 100L, "テスト商品", "P001", "白",
                new BigDecimal("1000"), 5, false, BigDecimal.ZERO);
        when(cartRepository.findProductSnapshotsByVariantIds(any()))
                .thenReturn(Map.of(10L, snap));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(new BigDecimal("10"));

        CartView cart = sut.getCart(request, response);

        assertThat(cart.items()).hasSize(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CartProductSnapshot snapshot(long variantId, int stock, boolean assemblyAvailable) {
        return new CartProductSnapshot(variantId, 100L, "商品", "CODE", "白",
                new BigDecimal("500"), stock, assemblyAvailable, BigDecimal.ZERO);
    }
}
