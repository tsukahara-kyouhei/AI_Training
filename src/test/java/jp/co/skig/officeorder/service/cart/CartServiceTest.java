package jp.co.skig.officeorder.service.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import jp.co.skig.officeorder.testutil.TestMessageSourceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private final CartCookieStore cartCookieStore = mock(CartCookieStore.class);
    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CartService service = new CartService(
            cartCookieStore,
            cartRepository,
            TestMessageSourceFactory.create()
    );

    /**
     * getCart で Cookie 明細の数量・組立指定・存在しない商品が正規化され、金額サマリーまで再計算されることを確認する。
     */
    @Test
    void getCart_normalizesCookieItemsAndBuildsSummary() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<CartCookieItem> rawItems = List.of(
                new CartCookieItem(1L, 0, true),
                new CartCookieItem(2L, 2, true),
                new CartCookieItem(3L, 1, false)
        );
        when(cartCookieStore.load(request)).thenReturn(rawItems);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L, 2L, 3L))).thenReturn(Map.of(
                1L, snapshot(1L, 10L, "Nordis ワークデスク 幅120cm", "P0001-C01", "ホワイト",
                        1000, 5, false, 300),
                2L, snapshot(2L, 20L, "Lattice チェア ハイバック", "P0002-C02", "ブラック",
                        500, 0, true, 300)
        ));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView actual = service.getCart(request, response);

        assertThat(actual.itemTypeCount()).isEqualTo(2);
        assertThat(actual.totalQuantity()).isEqualTo(3);
        assertThat(actual.summary().productSubtotal()).isEqualByComparingTo("2000");
        assertThat(actual.summary().assemblyFeeTotal()).isEqualByComparingTo("600");
        assertThat(actual.summary().taxAmount()).isEqualByComparingTo("260");
        assertThat(actual.summary().shippingFee()).isEqualByComparingTo("800");
        assertThat(actual.summary().totalAmount()).isEqualByComparingTo("3660");
        assertThat(actual.items())
                .extracting(item -> item.detailUrl())
                .containsExactly("/products/10", "/products/20?stock=out");
        assertThat(actual.items())
                .extracting(item -> item.assemblyRequested())
                .containsExactly(false, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(any(), any(), captor.capture());
        assertThat(captor.getValue()).containsExactly(
                new CartCookieItem(1L, 1, null),
                new CartCookieItem(2L, 2, true)
        );
    }

    /**
     * addItem で既存明細がある場合は数量を加算し、上限99件で打ち止めしつつ組立指定を上書きできることを確認する。
     */
    @Test
    void addItem_mergesExistingLineAndCapsQuantityAtNinetyNine() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 98, false)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, snapshot(1L, 10L, "Nordis ワークデスク 幅120cm", "P0001-C01", "ホワイト",
                        1000, 4, true, 300)
        ));

        service.addItem(request, response, 1L, 5, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(any(), any(), captor.capture());
        assertThat(captor.getValue()).containsExactly(new CartCookieItem(1L, 99, true));
    }

    /**
     * 同一リクエスト内で複数回 addItem を呼んだ場合でも、request attribute の最新状態を使って全明細が保持されることを確認する。
     */
    @Test
    void addItem_usesRequestScopedCartStateAcrossMultipleCalls() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, snapshot(1L, 10L, "Nordis ワークデスク 幅120cm", "P0001-C01", "ホワイト",
                        1000, 5, true, 300)
        ));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(2L))).thenReturn(Map.of(
                2L, snapshot(2L, 20L, "Lattice チェア ハイバック", "P0002-C02", "ブラック",
                        800, 8, true, 300)
        ));

        service.addItem(request, response, 1L, 1, false);
        service.addItem(request, response, 2L, 2, true);

        verify(cartCookieStore, times(1)).load(request);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore, times(2)).save(any(), any(), captor.capture());
        assertThat(captor.getAllValues().get(1)).containsExactly(
                new CartCookieItem(1L, 1, false),
                new CartCookieItem(2L, 2, true)
        );
    }

    /**
     * updateItem で組立対象外の商品を更新した場合は、数量だけを更新して組立指定は null に正規化されることを確認する。
     */
    @Test
    void updateItem_normalizesAssemblyWhenProductIsNotAssemblyAvailable() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(new CartCookieItem(1L, 1, false)));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, snapshot(1L, 10L, "Nordis ワークデスク 幅120cm", "P0001-C01", "ホワイト",
                        1000, 5, false, 300)
        ));

        service.updateItem(request, response, 1L, 4, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(any(), any(), captor.capture());
        assertThat(captor.getValue()).containsExactly(new CartCookieItem(1L, 4, null));
    }

    /**
     * removeItem で指定した商品だけが削除され、残りの明細はそのまま保存されることを確認する。
     */
    @Test
    void removeItem_removesOnlyTargetLine() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 1, false),
                new CartCookieItem(2L, 2, true)
        ));

        service.removeItem(request, response, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(any(), any(), captor.capture());
        assertThat(captor.getValue()).containsExactly(new CartCookieItem(2L, 2, true));
    }

    /**
     * 在庫切れ商品を addItem しようとした場合は、保存せず業務例外で拒否することを確認する。
     */
    @Test
    void addItem_rejectsOutOfStockProduct() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(
                1L, snapshot(1L, 10L, "Nordis ワークデスク 幅120cm", "P0001-C01", "ホワイト",
                        1000, 0, true, 300)
        ));

        assertThatThrownBy(() -> service.addItem(request, response, 1L, 1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("在庫が不足しているため注文できません。");
    }

    private CartProductSnapshot snapshot(long productVariantId,
                                         long productId,
                                         String productName,
                                         String productCode,
                                         String colorName,
                                         long unitPrice,
                                         int stockQuantity,
                                         boolean assemblyAvailable,
                                         long assemblyFee) {
        return new CartProductSnapshot(
                productVariantId,
                productId,
                productName,
                productCode,
                colorName,
                BigDecimal.valueOf(unitPrice),
                stockQuantity,
                assemblyAvailable,
                BigDecimal.valueOf(assemblyFee)
        );
    }
}
