package jp.co.skig.officeorder.service.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

/**
 * {@link CartService} のユニットテスト。
 *
 * <p>
 * C-01〜C-39 のテストケースを網羅する。
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  private CartCookieStore cartCookieStore;
  private CartRepository cartRepository;
  private CartService service;
  private HttpServletRequest request;
  private HttpServletResponse response;

  @BeforeEach
  void setUp() {
    cartCookieStore = mock(CartCookieStore.class);
    cartRepository = mock(CartRepository.class);
    MessageSource ms = mock(MessageSource.class);
    lenient().when(ms.getMessage(any(String.class), any(Object[].class), any(Locale.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    service = new CartService(cartCookieStore, cartRepository, ms);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
  }

  // ============================================================
  // C-01〜C-17: addItem
  // ============================================================

  @Nested
  @DisplayName("addItem")
  class AddItem {

    @Test
    @DisplayName("C-01: productVariantId=0 のとき IllegalArgumentException（invalidProduct）")
    void variantIdZero_throwsInvalidProduct() {
      assertThatThrownBy(() -> service.addItem(request, response, 0L, 1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-02: productVariantId=-1 のとき IllegalArgumentException（invalidProduct）")
    void variantIdNegative_throwsInvalidProduct() {
      assertThatThrownBy(() -> service.addItem(request, response, -1L, 1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-03: productVariantId=1（最小有効値）のとき追加成功")
    void variantIdOne_success() {
      setupEmptyCart();
      setupSnapshot(1L, 5);

      service.addItem(request, response, 1L, 1, null);

      verifyCartSaved();
    }

    @Test
    @DisplayName("C-04: quantity=0 のとき IllegalArgumentException（quantityRange）")
    void quantityZero_throwsQuantityRange() {
      assertThatThrownBy(() -> service.addItem(request, response, 1L, 0, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-05: quantity=1（最小）のとき追加成功")
    void quantityOne_success() {
      setupEmptyCart();
      setupSnapshot(1L, 5);

      service.addItem(request, response, 1L, 1, null);

      verifyCartSaved();
    }

    @Test
    @DisplayName("C-06: quantity=99（最大）のとき追加成功")
    void quantityNinetyNine_success() {
      setupEmptyCart();
      setupSnapshot(1L, 100);

      service.addItem(request, response, 1L, 99, null);

      verifyCartSaved();
    }

    @Test
    @DisplayName("C-07: quantity=100（最大+1）のとき IllegalArgumentException（quantityRange）")
    void quantityOneHundred_throwsQuantityRange() {
      assertThatThrownBy(() -> service.addItem(request, response, 1L, 100, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-08: quantity=-1 のとき IllegalArgumentException（quantityRange）")
    void quantityNegative_throwsQuantityRange() {
      assertThatThrownBy(() -> service.addItem(request, response, 1L, -1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-09: 在庫数量=0 のとき IllegalArgumentException（stockShortage）")
    void stockZero_throwsStockShortage() {
      setupSnapshotForFindOnly(1L, 0);

      assertThatThrownBy(() -> service.addItem(request, response, 1L, 1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-10: 在庫数量=1 のとき追加成功")
    void stockOne_success() {
      setupEmptyCart();
      setupSnapshot(1L, 1);

      service.addItem(request, response, 1L, 1, null);

      verifyCartSaved();
    }

    @Test
    @DisplayName("C-11: snapshot が存在しない（商品不存在）とき IllegalArgumentException（loadFailed）")
    void snapshotMissing_throwsLoadFailed() {
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
          .thenReturn(Map.of());

      assertThatThrownBy(() -> service.addItem(request, response, 1L, 1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-12: 同一バリアントが既存のとき数量が合算される（合計 < 99）")
    void existingItem_mergesQuantity() {
      CartCookieItem existing = new CartCookieItem(1L, 3, null);
      setupCartWith(List.of(existing));
      setupSnapshot(1L, 50);

      service.addItem(request, response, 1L, 5, null);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      List<CartCookieItem> saved = captor.getValue();
      assertThat(saved).hasSize(1);
      assertThat(saved.get(0).quantity()).isEqualTo(8); // 3 + 5
    }

    @Test
    @DisplayName("C-13: 同一バリアントが既存のとき合計が 99 を超えると 99 にクランプされる")
    void existingItem_mergeClampsToMax() {
      CartCookieItem existing = new CartCookieItem(1L, 95, null);
      setupCartWith(List.of(existing));
      setupSnapshot(1L, 100);

      service.addItem(request, response, 1L, 10, null);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      assertThat(captor.getValue().get(0).quantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("C-14: 新規バリアント追加でカートが 100 件（満杯）のとき IllegalArgumentException（limitExceeded）")
    void cartFull_throwsLimitExceeded() {
      List<CartCookieItem> fullCart = buildCart(100);
      setupCartWith(fullCart);
      setupSnapshotForFindOnly(101L, 5);

      assertThatThrownBy(() -> service.addItem(request, response, 101L, 1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-15: 新規バリアント追加でカートが 99 件（満杯-1）のとき追加成功（100件になる）")
    void cartAlmostFull_success() {
      List<CartCookieItem> almostFull = buildCart(99);
      setupCartWith(almostFull);
      setupSnapshot(101L, 5);

      service.addItem(request, response, 101L, 1, null);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      assertThat(captor.getValue()).hasSize(100);
    }

    @Test
    @DisplayName("C-16: assemblyRequested=true かつ assembly不可のとき null に正規化される")
    void assemblyTrueButUnavailable_savedAsNull() {
      setupEmptyCart();
      // assemblyAvailable = false → normalizeAssembly returns null
      CartProductSnapshot snapshot = snapshot(1L, 5, false);
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
          .thenReturn(Map.of(1L, snapshot));

      service.addItem(request, response, 1L, 1, Boolean.TRUE);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      assertThat(captor.getValue().get(0).assemblyRequested()).isNull();
    }

    @Test
    @DisplayName("C-17: assemblyRequested=null かつ assembly可能のとき false に正規化される")
    void assemblyNullButAvailable_savedAsFalse() {
      setupEmptyCart();
      // assemblyAvailable = true, requested = null → Boolean.TRUE.equals(null) =
      // false
      CartProductSnapshot snapshot = snapshot(1L, 5, true);
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
          .thenReturn(Map.of(1L, snapshot));

      service.addItem(request, response, 1L, 1, null);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      assertThat(captor.getValue().get(0).assemblyRequested()).isFalse();
    }
  }

  // ============================================================
  // C-18〜C-23: updateItem
  // ============================================================

  @Nested
  @DisplayName("updateItem")
  class UpdateItem {

    @Test
    @DisplayName("C-18: quantity=0 のとき IllegalArgumentException（quantityRange）")
    void quantityZero_throwsQuantityRange() {
      assertThatThrownBy(() -> service.updateItem(request, response, 1L, 0, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-19: quantity=1（最小）のとき更新成功")
    void quantityOne_success() {
      setupCartWith(List.of(new CartCookieItem(1L, 5, null)));
      setupSnapshot(1L, 10);

      service.updateItem(request, response, 1L, 1, null);

      verifyCartSaved();
    }

    @Test
    @DisplayName("C-20: quantity=99（最大）のとき更新成功")
    void quantityNinetyNine_success() {
      setupCartWith(List.of(new CartCookieItem(1L, 5, null)));
      setupSnapshot(1L, 100);

      service.updateItem(request, response, 1L, 99, null);

      verifyCartSaved();
    }

    @Test
    @DisplayName("C-21: quantity=100（最大+1）のとき IllegalArgumentException（quantityRange）")
    void quantityOneHundred_throwsQuantityRange() {
      assertThatThrownBy(() -> service.updateItem(request, response, 1L, 100, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C-22: 対象バリアントがカートにない場合は何もしない（save は呼ばれない）")
    void variantNotInCart_noOp() {
      setupCartWith(List.of(new CartCookieItem(2L, 5, null)));

      service.updateItem(request, response, 1L, 3, null);

      verify(cartCookieStore, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("C-23: 在庫数量=0 のとき IllegalArgumentException（stockShortage）")
    void stockZero_throwsStockShortage() {
      setupCartWith(List.of(new CartCookieItem(1L, 3, null)));
      setupSnapshot(1L, 0);

      assertThatThrownBy(() -> service.updateItem(request, response, 1L, 1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // ============================================================
  // C-24〜C-28: getCart（送料計算）
  // ============================================================

  @Nested
  @DisplayName("getCart（送料計算）")
  class GetCart {

    @Test
    @DisplayName("C-24: カートが空のとき商品小計・税額がゼロで送料のみ発生する")
    void emptyCart_productAmountsZeroShippingCharged() {
      when(request.getAttribute(anyString())).thenReturn(null);
      when(cartCookieStore.load(request)).thenReturn(List.of());
      when(cartRepository.findProductSnapshotsByVariantIds(List.of()))
          .thenReturn(Map.of());
      when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

      CartView cart = service.getCart(request, response);

      assertThat(cart.items()).isEmpty();
      assertThat(cart.summary().productSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(cart.summary().taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
      // 税込合計 0 円 < 5000 円なので送料 800 円が発生する（コード実装の正しい動作）
      assertThat(cart.summary().shippingFee()).isEqualByComparingTo(new BigDecimal("800"));
    }

    @Test
    @DisplayName("C-25: 税込合計が 4_999 円のとき送料が 800 円")
    void taxInclusiveTotal4999_shippingFee800() {
      // unitPrice=4545, taxRate=10%: taxable=4545, tax=floor(454.5)=454,
      // shippingTarget=4999
      CartCookieItem cookieItem = new CartCookieItem(1L, 1, null);
      CartProductSnapshot snap = snapshot(1L, new BigDecimal("4545"), 10, false);
      when(request.getAttribute(anyString())).thenReturn(null);
      when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
          .thenReturn(Map.of(1L, snap));
      when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

      CartView cart = service.getCart(request, response);

      assertThat(cart.summary().shippingFee()).isEqualByComparingTo(new BigDecimal("800"));
    }

    @Test
    @DisplayName("C-26: 税込合計が 5_000 円のとき送料が 0 円")
    void taxInclusiveTotal5000_shippingFeeZero() {
      // unitPrice=4546, taxRate=10%: taxable=4546, tax=floor(454.6)=454,
      // shippingTarget=5000
      CartCookieItem cookieItem = new CartCookieItem(1L, 1, null);
      CartProductSnapshot snap = snapshot(1L, new BigDecimal("4546"), 10, false);
      when(request.getAttribute(anyString())).thenReturn(null);
      when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
          .thenReturn(Map.of(1L, snap));
      when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

      CartView cart = service.getCart(request, response);

      assertThat(cart.summary().shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("C-27: 税込合計が 5_001 円のとき送料が 0 円")
    void taxInclusiveTotal5001_shippingFeeZero() {
      // unitPrice=4547, taxRate=10%: taxable=4547, tax=floor(454.7)=454,
      // shippingTarget=5001
      CartCookieItem cookieItem = new CartCookieItem(1L, 1, null);
      CartProductSnapshot snap = snapshot(1L, new BigDecimal("4547"), 10, false);
      when(request.getAttribute(anyString())).thenReturn(null);
      when(cartCookieStore.load(request)).thenReturn(List.of(cookieItem));
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L)))
          .thenReturn(Map.of(1L, snap));
      when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

      CartView cart = service.getCart(request, response);

      assertThat(cart.summary().shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("C-28: Cookie内バリアントIDに対応する snapshot がない場合はその明細をスキップする")
    void missingSnapshot_lineSkipped() {
      CartCookieItem item1 = new CartCookieItem(1L, 1, null);
      CartCookieItem item2 = new CartCookieItem(2L, 1, null);
      CartProductSnapshot snapForItem1 = snapshot(1L, new BigDecimal("1000"), 5, false);
      when(request.getAttribute(anyString())).thenReturn(null);
      when(cartCookieStore.load(request)).thenReturn(List.of(item1, item2));
      when(cartRepository.findProductSnapshotsByVariantIds(List.of(1L, 2L)))
          .thenReturn(Map.of(1L, snapForItem1)); // item2 のスナップショットなし
      when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

      CartView cart = service.getCart(request, response);

      assertThat(cart.items()).hasSize(1); // item2 はスキップされ item1 だけ
    }
  }

  // ============================================================
  // C-29〜C-36: sanitizeRedirectPath
  // ============================================================

  @Nested
  @DisplayName("sanitizeRedirectPath")
  class SanitizeRedirectPath {

    @Test
    @DisplayName("C-29: null のとき null が返る")
    void nullPath_returnsNull() {
      assertThat(service.sanitizeRedirectPath(null)).isNull();
    }

    @Test
    @DisplayName("C-30: 空文字のとき null が返る")
    void emptyPath_returnsNull() {
      assertThat(service.sanitizeRedirectPath("")).isNull();
    }

    @Test
    @DisplayName("C-31: 空白のみのとき null が返る")
    void blankPath_returnsNull() {
      assertThat(service.sanitizeRedirectPath("   ")).isNull();
    }

    @Test
    @DisplayName("C-32: スラッシュ始まりの相対パスは有効")
    void slashStartPath_valid() {
      assertThat(service.sanitizeRedirectPath("/cart")).isEqualTo("/cart");
    }

    @Test
    @DisplayName("C-33: // 始まりのパスは null（オープンリダイレクト防止）")
    void doubleSlashPath_returnsNull() {
      assertThat(service.sanitizeRedirectPath("//evil.example.com")).isNull();
    }

    @Test
    @DisplayName("C-34: :// を含むパスは null（オープンリダイレクト防止）")
    void schemeColonPath_returnsNull() {
      assertThat(service.sanitizeRedirectPath("http://evil.example.com")).isNull();
    }

    @Test
    @DisplayName("C-35: スラッシュなしのパスは null")
    void noSlashPath_returnsNull() {
      assertThat(service.sanitizeRedirectPath("cart")).isNull();
    }

    @Test
    @DisplayName("C-36: 前後空白がトリムされて有効パスになる")
    void pathWithWhitespace_trimmed() {
      assertThat(service.sanitizeRedirectPath("  /cart  ")).isEqualTo("/cart");
    }
  }

  // ============================================================
  // C-37〜C-39: removeItem / clear
  // ============================================================

  @Nested
  @DisplayName("removeItem / clear")
  class RemoveItemAndClear {

    @Test
    @DisplayName("C-37: 存在する明細が削除される（save が呼ばれる）")
    void existingItem_removed() {
      CartCookieItem item = new CartCookieItem(1L, 3, null);
      setupCartWith(List.of(item));

      service.removeItem(request, response, 1L);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      assertThat(captor.getValue()).isEmpty(); // 1件削除 → 空
    }

    @Test
    @DisplayName("C-38: 存在しない明細を削除しても正常終了（save が呼ばれ、カートは変化なし）")
    void nonExistingItem_noChangeButSaved() {
      CartCookieItem item = new CartCookieItem(2L, 1, null);
      setupCartWith(List.of(item));

      service.removeItem(request, response, 1L); // ID=1 はカートにない

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
      assertThat(captor.getValue()).hasSize(1); // 変化なし
    }

    @Test
    @DisplayName("C-39: clear を呼ぶと cartCookieStore.clear が呼ばれる")
    void clear_callsCookieStoreClear() {
      service.clear(request, response);

      verify(cartCookieStore).clear(request, response);
    }
  }

  // ============================================================
  // ヘルパメソッド
  // ============================================================

  /**
   * request の attribute キャッシュミス + 空カートをセットアップする。
   *
   * <p>
   * addItem 等の全ステップを通る happy path テストで使用する。
   */
  private void setupEmptyCart() {
    lenient().when(request.getAttribute(anyString())).thenReturn(null);
    lenient().when(cartCookieStore.load(request)).thenReturn(List.of());
  }

  /**
   * request の attribute キャッシュミス + 指定アイテムのカートをセットアップする。
   */
  private void setupCartWith(List<CartCookieItem> items) {
    lenient().when(request.getAttribute(anyString())).thenReturn(null);
    lenient().when(cartCookieStore.load(request)).thenReturn(items);
  }

  /**
   * addItem の findSnapshot 呼び出しで使われる snapshot のみセットアップする。
   *
   * <p>
   * 在庫チェック前に例外が発生するケースでは、
   * cart のロード（loadCurrentCartItems）は呼ばれないため、このメソッドを使う。
   */
  private void setupSnapshotForFindOnly(long variantId, int stock) {
    CartProductSnapshot snap = snapshot(variantId, new BigDecimal("1000"), stock, false);
    when(cartRepository.findProductSnapshotsByVariantIds(List.of(variantId)))
        .thenReturn(stock <= 0 ? Map.of(variantId, snap) : Map.of(variantId, snap));
  }

  /**
   * addItem の happy path 全体で使われる snapshot をセットアップする。
   */
  private void setupSnapshot(long variantId, int stock) {
    CartProductSnapshot snap = snapshot(variantId, new BigDecimal("1000"), stock, false);
    lenient().when(cartRepository.findProductSnapshotsByVariantIds(List.of(variantId)))
        .thenReturn(Map.of(variantId, snap));
  }

  /** カート保存（save）が呼ばれたことを検証する。 */
  private void verifyCartSaved() {
    verify(cartCookieStore).save(eq(request), eq(response), any());
  }

  /** assembly 情報を指定して商品スナップショットを生成する。 */
  private static CartProductSnapshot snapshot(long variantId, int stock, boolean assemblyAvailable) {
    return snapshot(variantId, new BigDecimal("1000"), stock, assemblyAvailable);
  }

  private static CartProductSnapshot snapshot(long variantId, BigDecimal unitPrice, int stock,
      boolean assemblyAvailable) {
    return new CartProductSnapshot(
        variantId, 10L, "テスト商品", "P001-C01", "ナチュラル",
        unitPrice, stock, assemblyAvailable, BigDecimal.ZERO);
  }

  /** 指定件数の異なる variantId を持つカートアイテムリストを生成する。 */
  private static List<CartCookieItem> buildCart(int count) {
    List<CartCookieItem> items = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      items.add(new CartCookieItem((long) i, 1, null));
    }
    return items;
  }
}
