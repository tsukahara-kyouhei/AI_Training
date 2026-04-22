package jp.co.skig.officeorder.service.cart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CartService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartCookieStore cartCookieStore;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MessageSource messageSource;

    private CartService sut;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        sut = new CartService(cartCookieStore, cartRepository, messageSource);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        lenient().when(messageSource.getMessage(any(String.class), any(), any(Locale.class))).thenReturn("error");
    }

    // =========================================================
    // sanitizeRedirectPath
    // =========================================================

    // ---- SR-01: null → null ----
    @Test
    @DisplayName("null パスは null を返す")
    void sr01_null_returnsNull() {
        assertThat(sut.sanitizeRedirectPath(null)).isNull();
    }

    // ---- SR-02: 空文字 → null ----
    @Test
    @DisplayName("空文字パスは null を返す")
    void sr02_blank_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("")).isNull();
    }

    // ---- SR-03: "/" 始まりでない → null ----
    @Test
    @DisplayName("'/' で始まらないパスは null を返す")
    void sr03_noLeadingSlash_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("products/1")).isNull();
    }

    // ---- SR-04: "//" 始まり → null ----
    @Test
    @DisplayName("'//' で始まるパスは null を返す（プロトコル相対URL対策）")
    void sr04_doubleSlash_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("//evil.example.com")).isNull();
    }

    // ---- SR-05: "://" を含む → null ----
    @Test
    @DisplayName("'://' を含むパスは null を返す（絶対URL対策）")
    void sr05_absoluteUrl_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("https://evil.example.com")).isNull();
    }

    // ---- SR-06: 正常な相対パス → そのまま返す ----
    @Test
    @DisplayName("有効な相対パスはそのまま返す")
    void sr06_validRelativePath_returnsSame() {
        assertThat(sut.sanitizeRedirectPath("/products/123")).isEqualTo("/products/123");
    }

    // ---- SR-07: 前後空白付きパス → trim後に返す ----
    @Test
    @DisplayName("前後に空白があるパスは trim した値を返す")
    void sr07_pathWithWhitespace_returnsTrimmed() {
        assertThat(sut.sanitizeRedirectPath("  /cart  ")).isEqualTo("/cart");
    }

    // ---- SR-08: "/" のみ → そのまま返す ----
    @Test
    @DisplayName("'/' のみはルートパスとして返す")
    void sr08_rootPath_returnsSame() {
        assertThat(sut.sanitizeRedirectPath("/")).isEqualTo("/");
    }

    // =========================================================
    // addItem
    // =========================================================

    // ---- AI-01: productVariantId <= 0 → 例外 ----
    @Test
    @DisplayName("productVariantId が 0 以下の場合は例外をスローする")
    void ai01_invalidVariantId_throwsException() {
        assertThatThrownBy(() -> sut.addItem(request, response, 0L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- AI-02: quantity < 1 → 例外 ----
    @Test
    @DisplayName("数量が 0 以下の場合は例外をスローする")
    void ai02_quantityBelowMinimum_throwsException() {
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- AI-03: quantity > 99 → 例外 ----
    @Test
    @DisplayName("数量が 100 以上の場合は例外をスローする")
    void ai03_quantityAboveMaximum_throwsException() {
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- AI-04: 在庫なし商品 → 例外 ----
    @Test
    @DisplayName("在庫切れ商品を追加しようとすると例外をスローする")
    void ai04_outOfStock_throwsException() {
        CartProductSnapshot snapshot = makeSnapshot(1L, 0, false, null);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- AI-05: 新規追加（カートが空）→ Cookieに保存される ----
    @Test
    @DisplayName("空カートへの新規追加が正常に保存される")
    void ai05_addToEmptyCart_itemSaved() {
        CartProductSnapshot snapshot = makeSnapshot(1L, 5, false, null);
        when(cartCookieStore.load(request)).thenReturn(List.of());
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        sut.addItem(request, response, 1L, 2, null);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // ---- AI-06: 同一商品が既存の場合は数量を合算する（上限99）----
    @Test
    @DisplayName("既存アイテムに追加すると数量が合算され上限99に丸まる")
    void ai06_addExistingItem_quantityMergedWithCap() {
        CartCookieItem existing = new CartCookieItem(1L, 95, null);
        CartProductSnapshot snapshot = makeSnapshot(1L, 100, false, null);
        when(cartCookieStore.load(request)).thenReturn(List.of(existing));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        // 95 + 10 = 105 → 99に丸まる
        sut.addItem(request, response, 1L, 10, null);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // ---- AI-07: MAX_LINE_ITEMS 超過の場合は例外をスローする ----
    @Test
    @DisplayName("カート明細数が上限100件を超える場合は例外をスローする")
    void ai07_limitExceeded_throwsException() {
        // 100件の別アイテムをモック (variantId 1L..100L)
        List<CartCookieItem> fullCart = java.util.stream.LongStream.rangeClosed(1, 100)
                .mapToObj(id -> new CartCookieItem(id, 1, null))
                .toList();
        CartProductSnapshot snapshot = makeSnapshot(999L, 5, false, null);
        when(cartCookieStore.load(request)).thenReturn(fullCart);
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(999L)))
                .thenReturn(Map.of(999L, snapshot));

        assertThatThrownBy(() -> sut.addItem(request, response, 999L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================
    // updateItem
    // =========================================================

    // ---- UI-01: quantity < 1 → 例外 ----
    @Test
    @DisplayName("更新数量が 0 以下の場合は例外をスローする")
    void ui01_quantityBelowMin_throwsException() {
        assertThatThrownBy(() -> sut.updateItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- UI-02: quantity > 99 → 例外 ----
    @Test
    @DisplayName("更新数量が 100 以上の場合は例外をスローする")
    void ui02_quantityAboveMax_throwsException() {
        assertThatThrownBy(() -> sut.updateItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- UI-03: カートに存在しない商品 → 何もしない ----
    @Test
    @DisplayName("カートに存在しない商品IDで更新しても例外は出ない")
    void ui03_notFound_noop() {
        when(cartCookieStore.load(request)).thenReturn(List.of());

        // 例外がスローされないことを確認
        sut.updateItem(request, response, 99L, 1, null);
    }

    // ---- UI-04: 在庫なし商品の更新 → 例外 ----
    @Test
    @DisplayName("在庫切れ商品の更新は例外をスローする")
    void ui04_outOfStock_throwsException() {
        CartCookieItem item = new CartCookieItem(1L, 3, null);
        CartProductSnapshot snapshot = makeSnapshot(1L, 0, false, null);
        when(cartCookieStore.load(request)).thenReturn(List.of(item));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        assertThatThrownBy(() -> sut.updateItem(request, response, 1L, 2, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- UI-05: 正常更新 → Cookieに保存される ----
    @Test
    @DisplayName("有効な更新は Cookie に保存される")
    void ui05_validUpdate_itemSaved() {
        CartCookieItem item = new CartCookieItem(1L, 3, null);
        CartProductSnapshot snapshot = makeSnapshot(1L, 10, false, null);
        when(cartCookieStore.load(request)).thenReturn(List.of(item));
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
                .thenReturn(Map.of(1L, snapshot));

        sut.updateItem(request, response, 1L, 5, null);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // =========================================================
    // removeItem
    // =========================================================

    // ---- RI-01: 存在する商品を削除 → Cookieが更新される ----
    @Test
    @DisplayName("存在するアイテムを削除すると Cookie が更新される")
    void ri01_removeExistingItem_cookieUpdated() {
        CartCookieItem item = new CartCookieItem(1L, 2, null);
        when(cartCookieStore.load(request)).thenReturn(List.of(item));

        sut.removeItem(request, response, 1L);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // ---- RI-02: 存在しない商品IDを削除しても例外は出ない ----
    @Test
    @DisplayName("存在しない商品IDの削除は例外なく完了する")
    void ri02_removeNonExistentItem_noop() {
        when(cartCookieStore.load(request)).thenReturn(List.of());

        sut.removeItem(request, response, 99L);

        verify(cartCookieStore).save(any(), any(), anyList());
    }

    // =========================================================
    // clear
    // =========================================================

    // ---- CL-01: clear は cartCookieStore.clear を呼ぶ ----
    @Test
    @DisplayName("clear はカートCookieを全削除する")
    void cl01_clear_callsCookieStoreClear() {
        sut.clear(request, response);

        verify(cartCookieStore).clear(request, response);
    }

    // =========================================================
    // countTotalQuantity
    // =========================================================

    // ---- CT-01: 空カート → 0 ----
    @Test
    @DisplayName("空カートの総数量は 0 を返す")
    void ct01_emptyCart_returnsZero() {
        when(cartCookieStore.load(request)).thenReturn(List.of());

        int count = sut.countTotalQuantity(request);

        assertThat(count).isEqualTo(0);
    }

    // ---- CT-02: 複数アイテム → 数量合計を返す ----
    @Test
    @DisplayName("複数アイテムの数量合計を返す")
    void ct02_multipleItems_returnsTotalQuantity() {
        List<CartCookieItem> items = List.of(
                new CartCookieItem(1L, 3, null),
                new CartCookieItem(2L, 7, null)
        );
        when(cartCookieStore.load(request)).thenReturn(items);

        int count = sut.countTotalQuantity(request);

        assertThat(count).isEqualTo(10);
    }

    // ---- CT-03: 数量 0 のアイテム → 1 に正規化されて合算される ----
    @Test
    @DisplayName("数量 0 のアイテムは 1 に正規化されて合計に加算される")
    void ct03_zeroQuantityItem_normalizedToOne() {
        List<CartCookieItem> items = List.of(
                new CartCookieItem(1L, 0, null)
        );
        when(cartCookieStore.load(request)).thenReturn(items);

        int count = sut.countTotalQuantity(request);

        assertThat(count).isEqualTo(1);
    }

    // =========================================================
    // helpers
    // =========================================================

    private CartProductSnapshot makeSnapshot(long variantId, int stock,
                                             boolean assemblyAvailable,
                                             BigDecimal assemblyFee) {
        return new CartProductSnapshot(
                variantId, 10L, "テスト商品", "P001", "ホワイト",
                new BigDecimal("5000"),
                stock,
                assemblyAvailable,
                assemblyFee
        );
    }
}
