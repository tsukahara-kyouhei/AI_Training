package jp.co.skig.officeorder.service.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private CartCookieStore cartCookieStore;
    private CartRepository cartRepository;
    private MessageSource messageSource;
    private CartService sut;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        cartCookieStore = mock(CartCookieStore.class);
        cartRepository = mock(CartRepository.class);
        messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("error");
        sut = new CartService(cartCookieStore, cartRepository, messageSource);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // --- sanitizeRedirectPath ---

    @Test
    @DisplayName("null のとき null を返す")
    void sanitizeRedirectPath_null_returnsNull() {
        assertThat(sut.sanitizeRedirectPath(null)).isNull();
    }

    @Test
    @DisplayName("空文字のとき null を返す")
    void sanitizeRedirectPath_empty_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("")).isNull();
    }

    @Test
    @DisplayName("空白のみのとき null を返す")
    void sanitizeRedirectPath_blank_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("   ")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "/products, /products",
        "/cart, /cart",
        "/products/123?foo=bar, /products/123?foo=bar",
        "  /products  , /products"
    })
    @DisplayName("/ で始まる正規の相対パスはそのまま（trim 済み）返す")
    void sanitizeRedirectPath_validRelativePath_returnsTrimmedPath(String input, String expected) {
        assertThat(sut.sanitizeRedirectPath(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("/ で始まらないパスは null を返す")
    void sanitizeRedirectPath_notStartingWithSlash_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("products/123")).isNull();
    }

    @Test
    @DisplayName("// で始まるパスは null を返す（プロトコル相対 URL 防止）")
    void sanitizeRedirectPath_doubleSlash_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("//evil.example.com")).isNull();
    }

    @Test
    @DisplayName(":// を含むパスは null を返す（絶対 URL 防止）")
    void sanitizeRedirectPath_containsProtocol_returnsNull() {
        assertThat(sut.sanitizeRedirectPath("/redirect?url=https://evil.example.com")).isNull();
    }

    // --- countTotalQuantity ---

    @Test
    @DisplayName("Cookie に複数明細があるとき数量合計を返す")
    void countTotalQuantity_multipleItems_returnsTotalQuantity() {
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 2, null),
                new CartCookieItem(2L, 3, true)
        ));

        assertThat(sut.countTotalQuantity(request)).isEqualTo(5);
    }

    @Test
    @DisplayName("Cookie が空のとき 0 を返す")
    void countTotalQuantity_empty_returnsZero() {
        when(cartCookieStore.load(request)).thenReturn(List.of());

        assertThat(sut.countTotalQuantity(request)).isZero();
    }

    // --- addItem ---

    @Test
    @DisplayName("productVariantId が 0 以下のとき IllegalArgumentException をスローする")
    void addItem_invalidProductId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> sut.addItem(request, response, 0L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("数量が 0 のとき IllegalArgumentException をスローする")
    void addItem_quantityZero_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("数量が 99 超のとき IllegalArgumentException をスローする")
    void addItem_quantityOver99_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("在庫 0 の商品を追加しようとすると IllegalArgumentException をスローする")
    void addItem_outOfStock_throwsIllegalArgumentException() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品A", "CODE-A", "白", BigDecimal.valueOf(1000), 0, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L))).thenReturn(Map.of(1L, snapshot));
        when(cartCookieStore.load(request)).thenReturn(List.of());

        assertThatThrownBy(() -> sut.addItem(request, response, 1L, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("新規商品を追加するとスナップショットを取得して Cookie に保存する")
    void addItem_newItem_savesItem() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品A", "CODE-A", "白", BigDecimal.valueOf(1000), 5, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(any())).thenReturn(Map.of(1L, snapshot));
        when(cartCookieStore.load(request)).thenReturn(List.of());

        sut.addItem(request, response, 1L, 2, null);

        verify(cartCookieStore).save(any(HttpServletRequest.class), any(HttpServletResponse.class), anyList());
    }

    @Test
    @DisplayName("既存明細に追加するとき数量を合算し上限 99 でキャップする")
    void addItem_existingItem_mergesQuantityWithCap() {
        CartProductSnapshot snapshot = new CartProductSnapshot(
                1L, 10L, "商品A", "CODE-A", "白", BigDecimal.valueOf(1000), 5, false, BigDecimal.ZERO
        );
        when(cartRepository.findProductSnapshotsByVariantIds(any())).thenReturn(Map.of(1L, snapshot));
        // 既存明細に 95 件入っている
        when(cartCookieStore.load(request)).thenReturn(List.of(
                new CartCookieItem(1L, 95, null)
        ));

        // 10 追加 → 95 + 10 = 105 > 99 → キャップされて 99
        sut.addItem(request, response, 1L, 10, null);

        verify(cartCookieStore).save(any(HttpServletRequest.class), any(HttpServletResponse.class),
                org.mockito.ArgumentMatchers.argThat(items -> {
                    List<CartCookieItem> list = (List<CartCookieItem>) items;
                    return list.size() == 1 && list.get(0).quantity() == 99;
                }));
    }

    // --- clear ---

    @Test
    @DisplayName("clear は Cookie を削除する")
    void clear_callsCookieStoreClear() {
        sut.clear(request, response);

        verify(cartCookieStore).clear(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    // --- パフォーマンス ---

    @Test
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    @DisplayName("sanitizeRedirectPath は 500ms 以内に完了する")
    void sanitizeRedirectPath_completesWithinTimeLimit() {
        for (int i = 0; i < 1000; i++) {
            sut.sanitizeRedirectPath("/products/" + i);
        }
    }
}
