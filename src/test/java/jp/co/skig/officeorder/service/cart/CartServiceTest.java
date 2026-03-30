package jp.co.skig.officeorder.service.cart;

import java.math.BigDecimal;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CartService の単体テスト。
 *
 * <p>税額・送料計算、addItem の数量加算・上限切り詰め、
 * スナップショット不在時の明細除外、数量/組立の正規化を検証する。
 */
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

    private CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0, String.class));
        cartService = new CartService(cartCookieStore, cartRepository, messageSource);
    }

    // -----------------------------------------------------------------------
    // 正常系：税額・送料の計算
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("税率10%・組立なし・1点3,000円で税額・合計が正しく計算される")
    void getCart_calculatesCorrectTaxAndTotal_whenSingleItemNoAssembly() {
        // 3000 * 10% = 300(切捨), shippingTarget=3300 < 5000 → 送料800
        stubCookieItems(List.of(new CartCookieItem(1L, 1, false)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(3_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.summary().taxAmount()).isEqualByComparingTo("300");
        assertThat(cart.summary().shippingFee()).isEqualByComparingTo("800");
        assertThat(cart.summary().totalAmount()).isEqualByComparingTo("4100");
    }

    @Test
    @DisplayName("税込5,000円以上で送料が0円になる（送料無料しきい値）")
    void getCart_zeroShippingFee_whenTaxIncludedTotalMeetsFreeShippingThreshold() {
        // 5000 * 10% = 500, shippingTarget=5500 >= 5000 → 送料0
        stubCookieItems(List.of(new CartCookieItem(1L, 1, false)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(5_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.summary().shippingFee()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("税込5,000円未満で送料が800円になる")
    void getCart_flatShippingFee800_whenTaxIncludedTotalBelowFreeShippingThreshold() {
        // 4000 * 10% = 400, shippingTarget=4400 < 5000 → 送料800
        stubCookieItems(List.of(new CartCookieItem(1L, 1, false)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(4_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.summary().shippingFee()).isEqualByComparingTo("800");
    }

    @Test
    @DisplayName("組立費が小計に加算されてから消費税・送料が計算される")
    void getCart_includesAssemblyFeeInTaxableBase_beforeCalculatingTaxAndShipping() {
        // 商品3000 + 組立500 = 3500, taxAmount=350, shippingTarget=3850 < 5000 → 送料800
        // totalAmount = 3500 + 350 + 800 = 4650
        stubCookieItems(List.of(new CartCookieItem(1L, 1, true)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(3_000), true, BigDecimal.valueOf(500))));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.summary().assemblyFeeTotal()).isEqualByComparingTo("500");
        assertThat(cart.summary().taxAmount()).isEqualByComparingTo("350");
        assertThat(cart.summary().totalAmount()).isEqualByComparingTo("4650");
    }

    @Test
    @DisplayName("複数明細の合計金額が正しく計算される")
    void getCart_calculatesCorrectTotalForMultipleItems() {
        // item1: 3000×2=6000, item2: 1500×3=4500, productSubtotal=10500
        // taxAmount=1050, shippingTarget=11550 >= 5000 → 送料0, total=11550
        stubCookieItems(List.of(
                new CartCookieItem(1L, 2, false),
                new CartCookieItem(2L, 3, false)
        ));
        stubSnapshots(Map.of(
                1L, buildSnapshot(1L, BigDecimal.valueOf(3_000), false, BigDecimal.ZERO),
                2L, buildSnapshot(2L, BigDecimal.valueOf(1_500), false, BigDecimal.ZERO)
        ));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.summary().productSubtotal()).isEqualByComparingTo("10500");
        assertThat(cart.summary().taxAmount()).isEqualByComparingTo("1050");
        assertThat(cart.summary().shippingFee()).isEqualByComparingTo("0");
        assertThat(cart.summary().totalAmount()).isEqualByComparingTo("11550");
    }

    // -----------------------------------------------------------------------
    // 正常系：addItem
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addItem で数量が既存明細に加算される")
    void addItem_mergesQuantityWithExistingItem_whenSameVariantAdded() {
        // 既存5個 + 追加3個 = 8個
        stubCookieItemsForLoad(List.of(new CartCookieItem(1L, 5, false)));
        stubSnapshotForVariant(1L, buildSnapshot(1L, BigDecimal.valueOf(1_000), false, BigDecimal.ZERO));

        cartService.addItem(request, response, 1L, 3, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("addItem で既存50+追加60=110は99に切り詰められる")
    void addItem_capsQuantityAt99_whenMergedExceedsMax() {
        // 既存50 + 追加60 = 110 → 99
        stubCookieItemsForLoad(List.of(new CartCookieItem(1L, 50, false)));
        stubSnapshotForVariant(1L, buildSnapshot(1L, BigDecimal.valueOf(1_000), false, BigDecimal.ZERO));

        cartService.addItem(request, response, 1L, 60, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartCookieItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartCookieStore).save(eq(request), eq(response), captor.capture());
        assertThat(captor.getValue().get(0).quantity()).isEqualTo(99);
    }

    // -----------------------------------------------------------------------
    // 正常系：スナップショット不在時の除外
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Cookieにスナップショットが存在しないバリアントは明細に含まれない")
    void getCart_excludesLineItem_whenSnapshotAbsentForVariant() {
        // variantId=9999 はスナップショット無し → カート明細に含まれない
        stubCookieItems(List.of(
                new CartCookieItem(1L, 1, false),
                new CartCookieItem(9999L, 1, false)
        ));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(3_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.itemTypeCount()).isEqualTo(1);
        assertThat(cart.items().get(0).productVariantId()).isEqualTo(1L);
    }

    // -----------------------------------------------------------------------
    // 境界値：数量・組立の正規化
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("数量0以下は1に正規化される")
    void getCart_normalizesQuantityZeroToOne_whenZeroQuantityInCookie() {
        stubCookieItems(List.of(new CartCookieItem(1L, 0, false)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(1_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.items().get(0).quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("数量99はそのまま維持される")
    void getCart_maintainsQuantity99_whenQuantityIsAtMax() {
        stubCookieItems(List.of(new CartCookieItem(1L, 99, false)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(1_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.items().get(0).quantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("組立不可商品で組立指定がfalseに正規化される")
    void getCart_normalizesAssemblyToFalse_whenAssemblyNotAvailableForProduct() {
        // assemblyAvailable=false なので assemblyRequested は null に正規化 → CartLineView では false
        stubCookieItems(List.of(new CartCookieItem(1L, 1, true)));
        stubSnapshots(Map.of(1L, buildSnapshot(1L, BigDecimal.valueOf(1_000), false, BigDecimal.ZERO)));
        when(cartRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);

        CartView cart = cartService.getCart(request, response);

        assertThat(cart.items().get(0).assemblyRequested()).isFalse();
    }

    // -----------------------------------------------------------------------
    // 異常系
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("存在しない商品バリアントIDを追加した場合はIllegalArgumentException")
    void addItem_throwsIllegalArgumentException_whenSnapshotNotFound() {
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(Map.of());

        assertThatThrownBy(() -> cartService.addItem(request, response, 999L, 1, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // ヘルパ
    // -----------------------------------------------------------------------

    /** getCart() 内で cartCookieStore.load() が呼ばれるときのスタブ。 */
    private void stubCookieItems(List<CartCookieItem> items) {
        when(cartCookieStore.load(request)).thenReturn(items);
    }

    /** addItem() 内で loadCurrentCartItems() が呼ばれるときのスタブ（load は lenient で再スタブ）。 */
    private void stubCookieItemsForLoad(List<CartCookieItem> items) {
        lenient().when(cartCookieStore.load(request)).thenReturn(items);
    }

    /** cartRepository.findProductSnapshotsByVariantIds(anyList()) のスタブ。 */
    private void stubSnapshots(Map<Long, CartProductSnapshot> snapshotMap) {
        when(cartRepository.findProductSnapshotsByVariantIds(anyList())).thenReturn(snapshotMap);
    }

    /**
     * addItem() 内の findSnapshot() が単一バリアントIDを渡す場合のスタブ。
     * getCart() での stubs とは独立して機能するよう lenient を使用。
     */
    private void stubSnapshotForVariant(long variantId, CartProductSnapshot snapshot) {
        lenient().when(cartRepository.findProductSnapshotsByVariantIds(anyList()))
                .thenReturn(Map.of(variantId, snapshot));
    }

    private CartProductSnapshot buildSnapshot(long variantId, BigDecimal unitPrice,
                                              boolean assemblyAvailable, BigDecimal assemblyFee) {
        return new CartProductSnapshot(
                variantId, 1L, "テスト商品", "P0001-C01", "ブラック",
                unitPrice, 10, assemblyAvailable, assemblyFee
        );
    }
}
