package jp.co.skig.officeorder.service.cart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//import jp.co.skig.officeorder.config.AppProperties.Cookie.Cart;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.cart.CartCookieItem;
import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartProductSnapshot;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.CouponForm;
import jp.co.skig.officeorder.repository.CartCookieStore;
import jp.co.skig.officeorder.repository.CartRepository;
import jp.co.skig.officeorder.repository.OrderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * カートCookieの内容を画面表示用データへ変換し、更新操作を仲介するサービス。
 *
 * <p>
 * 商品詳細からの追加、カート画面での更新・削除、ヘッダ件数表示のいずれも
 * ここを通すことで、数量上限・組立可否・送料計算を統一している。
 */
@Service
public class CartService {

    /** カート操作ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    /** 同一リクエスト中に最新カート状態を保持する request attribute 名。 */
    private static final String REQUEST_ATTR_CART_ITEMS = CartService.class.getName() + ".cartItems";

    /** 1明細あたりの最大数量。 */
    private static final int MAX_QUANTITY_PER_LINE = 99;
    /** 送料無料になる税込しきい値。 */
    private static final long FREE_SHIPPING_THRESHOLD_TAX_INCLUDED = 5000L;
    /** 送料の一律金額。 */
    private static final long FLAT_SHIPPING_FEE = 800L;

    /** カートCookieの読み書きを担当するストア。 */
    private final CartCookieStore cartCookieStore;
    /** カート表示用商品情報と税率取得を担当するリポジトリ。 */
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * カートサービスを生成する。
     *
     * @param cartCookieStore カートCookieの読み書き窓口
     * @param cartRepository  カート表示用商品情報の取得窓口
     * @param orderRepository
     * @param messageSource   利用者向けメッセージ取得元
     */
    public CartService(CartCookieStore cartCookieStore,
            CartRepository cartRepository,
            OrderRepository orderRepository,
            MessageSource messageSource) {
        this.cartCookieStore = cartCookieStore;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * 現在のカート内容を画面表示用に組み立てる。
     *
     * <p>
     * Cookie内の数量・組立指定を商品スナップショットと突き合わせて正規化し、
     * 商品小計、組立費、送料、消費税、合計金額を計算する。
     *
     * @param request  現在のHTTPリクエスト
     * @param response 現在のHTTPレスポンス
     * @return 表示用カート情報
     */
    public CartView getCart(HttpServletRequest request, HttpServletResponse response) {
        List<CartCookieItem> rawItems = loadCurrentCartItems(request);
        Map<Long, CartProductSnapshot> snapshots = cartRepository.findProductSnapshotsByVariantIds(
                rawItems.stream().map(CartCookieItem::productVariantId).toList());
        List<CartCookieItem> normalizedCookieItems = new ArrayList<>();
        List<CartLineView> lineViews = new ArrayList<>();

        BigDecimal productSubtotal = BigDecimal.ZERO;
        BigDecimal assemblySubtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (CartCookieItem rawItem : rawItems) {
            CartProductSnapshot snapshot = snapshots.get(rawItem.productVariantId());
            if (snapshot == null) {
                continue;
            }
            int quantity = normalizeQuantity(rawItem.quantity());
            Boolean assemblyRequested = normalizeAssembly(rawItem.assemblyRequested(), snapshot.assemblyAvailable());
            normalizedCookieItems.add(new CartCookieItem(snapshot.productVariantId(), quantity, assemblyRequested));

            BigDecimal lineProductSubtotal = snapshot.unitPrice().multiply(BigDecimal.valueOf(quantity));
            BigDecimal lineAssemblySubtotal = shouldApplyAssemblyFee(snapshot.assemblyAvailable(), assemblyRequested)
                    ? snapshot.assemblyFee().multiply(BigDecimal.valueOf(quantity))
                    : BigDecimal.ZERO;

            productSubtotal = productSubtotal.add(lineProductSubtotal);
            assemblySubtotal = assemblySubtotal.add(lineAssemblySubtotal);
            totalQuantity += quantity;

            lineViews.add(new CartLineView(
                    snapshot.productVariantId(),
                    snapshot.productId(),
                    snapshot.productName(),
                    snapshot.productCode(),
                    snapshot.colorName(),
                    snapshot.unitPrice(),
                    snapshot.stockQuantity(),
                    snapshot.assemblyAvailable(),
                    snapshot.assemblyFee(),
                    Boolean.TRUE.equals(assemblyRequested),
                    quantity,
                    buildProductDetailUrl(snapshot.productId(), snapshot.stockQuantity())));
        }

        if (!rawItems.equals(normalizedCookieItems)) {
            saveCurrentCartItems(request, response, normalizedCookieItems);
        }

        BigDecimal taxableSubtotal = productSubtotal.add(assemblySubtotal);
        BigDecimal discountAmount = getAppliedDiscountAmount(request, taxableSubtotal);
        //BigDecimal taxRate = cartRepository.findCurrentTaxRatePercent();

        BigDecimal taxAmount = BigDecimal.ZERO;

        //BigDecimal shippingTarget = taxableSubtotal;
        BigDecimal shippingFee = taxableSubtotal.compareTo(BigDecimal.valueOf(FREE_SHIPPING_THRESHOLD_TAX_INCLUDED)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(FLAT_SHIPPING_FEE);

        BigDecimal totalAmount = taxableSubtotal.add(shippingFee).subtract(discountAmount)
                .max(BigDecimal.ZERO);
        log.info(
                "cart summary: productSubtotal={}, assemblySubtotal={}, taxableSubtotal={}, taxAmount={}, shippingFee={}, discountAmount={}, totalAmount={}",
                productSubtotal,
                assemblySubtotal,
                taxableSubtotal,
                taxAmount,
                shippingFee,
                discountAmount,
                totalAmount);

        CartSummaryView summary = new CartSummaryView(
                productSubtotal,
                assemblySubtotal,
                shippingFee,
                discountAmount,
                taxAmount,
                totalAmount);
        return new CartView(lineViews, lineViews.size(), totalQuantity, summary);
    }

    /**
     * ヘッダ表示用にカート内総数量を数える。
     *
     * @param request 現在のHTTPリクエスト
     * @return 総数量
     */
    public int countTotalQuantity(HttpServletRequest request) {
        return cartCookieStore.load(request).stream()
                .mapToInt(item -> normalizeQuantity(item.quantity()))
                .sum();
    }

    /**
     * 商品をカートへ追加する。
     *
     * <p>
     * 同一商品が既に入っている場合は数量を加算し、
     * 組立指定は商品可否に応じて正規化する。
     *
     * @param request           現在のHTTPリクエスト
     * @param response          現在のHTTPレスポンス
     * @param productVariantId  追加対象の商品バリアントID
     * @param quantity          追加数量
     * @param assemblyRequested 組立指定
     */
    public void addItem(HttpServletRequest request,
            HttpServletResponse response,
            long productVariantId,
            int quantity,
            Boolean assemblyRequested) {
        if (productVariantId <= 0) {
            throw new IllegalArgumentException(message("business.cart.invalidProduct"));
        }
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException(message("business.cart.quantityRange"));
        }
        CartProductSnapshot snapshot = findSnapshot(productVariantId);
        if (snapshot.stockQuantity() <= 0) {
            throw new IllegalArgumentException(message("business.stockShortage"));
        }

        List<CartCookieItem> items = new ArrayList<>(loadCurrentCartItems(request));
        int existingIndex = findItemIndex(items, productVariantId);
        if (existingIndex >= 0) {
            CartCookieItem existing = items.get(existingIndex);
            int mergedQuantity = Math.min(MAX_QUANTITY_PER_LINE, existing.quantity() + quantity);
            Boolean mergedAssembly = normalizeAssembly(
                    assemblyRequested != null ? assemblyRequested : existing.assemblyRequested(),
                    snapshot.assemblyAvailable());
            items.set(existingIndex, new CartCookieItem(productVariantId, mergedQuantity, mergedAssembly));
        } else {
            if (items.size() >= CartCookieStore.MAX_LINE_ITEMS) {
                throw new IllegalArgumentException(message("business.cart.limitExceeded"));
            }
            items.add(new CartCookieItem(
                    productVariantId,
                    quantity,
                    normalizeAssembly(assemblyRequested, snapshot.assemblyAvailable())));
        }
        saveCurrentCartItems(request, response, deduplicate(items));
        log.info("event={} productVariantId={} quantity={} lineCount={}",
                LogEvent.CART_ITEM_ADDED.value(),
                productVariantId,
                quantity,
                items.size());
    }

    /**
     * 既存カート明細の数量・組立指定を更新する。
     *
     * @param request           現在のHTTPリクエスト
     * @param response          現在のHTTPレスポンス
     * @param productVariantId  更新対象の商品バリアントID
     * @param quantity          更新後数量
     * @param assemblyRequested 更新後組立指定
     */
    public void updateItem(HttpServletRequest request,
            HttpServletResponse response,
            long productVariantId,
            int quantity,
            Boolean assemblyRequested) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException(message("business.cart.quantityRange"));
        }
        List<CartCookieItem> items = new ArrayList<>(loadCurrentCartItems(request));
        int targetIndex = findItemIndex(items, productVariantId);
        if (targetIndex < 0) {
            return;
        }
        CartProductSnapshot snapshot = findSnapshot(productVariantId);
        if (snapshot.stockQuantity() <= 0) {
            throw new IllegalArgumentException(message("business.stockShortage"));
        }
        items.set(targetIndex, new CartCookieItem(
                productVariantId,
                quantity,
                normalizeAssembly(assemblyRequested, snapshot.assemblyAvailable())));
        saveCurrentCartItems(request, response, deduplicate(items));
        log.info("event={} productVariantId={} quantity={} lineCount={}",
                LogEvent.CART_ITEM_UPDATED.value(),
                productVariantId,
                quantity,
                items.size());
    }

    /**
     * 指定明細をカートから削除する。
     *
     * @param request          現在のHTTPリクエスト
     * @param response         現在のHTTPレスポンス
     * @param productVariantId 削除対象の商品バリアントID
     */
    public void removeItem(HttpServletRequest request, HttpServletResponse response, long productVariantId) {
        List<CartCookieItem> items = new ArrayList<>(loadCurrentCartItems(request));
        items.removeIf(item -> item.productVariantId() == productVariantId);
        saveCurrentCartItems(request, response, items);
        log.info("event={} productVariantId={} lineCount={}",
                LogEvent.CART_ITEM_REMOVED.value(),
                productVariantId,
                items.size());
    }

    /**
     * カートを空にする。
     *
     * @param request  現在のHTTPリクエスト
     * @param response 現在のHTTPレスポンス
     */
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        cartCookieStore.clear(request, response);
        rememberCurrentCartItems(request, List.of());
        log.info("event={}", LogEvent.CART_CLEARED.value());
    }

    /**
     * ログイン後戻り先に使える安全な相対パスだけを通す。
     *
     * @param rawPath 画面から受け取った戻り先候補
     * @return 利用可能な相対パス。無効な場合は {@code null}
     */
    public String sanitizeRedirectPath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        String path = rawPath.trim();
        if (!path.startsWith("/")) {
            return null;
        }
        if (path.startsWith("//")) {
            return null;
        }
        if (path.contains("://")) {
            return null;
        }
        return path;
    }

    /**
     * バリアントIDからカート表示用スナップショットを取得する。
     *
     * @param productVariantId 商品バリアントID
     * @return 商品スナップショット
     */
    private CartProductSnapshot findSnapshot(long productVariantId) {
        CartProductSnapshot snapshot = cartRepository.findProductSnapshotsByVariantIds(List.of(productVariantId))
                .get(productVariantId);
        if (snapshot == null) {
            throw new IllegalArgumentException(message("business.cart.loadFailed"));
        }
        return snapshot;
    }

    /**
     * 利用者向けメッセージを取得する。
     *
     * @param code メッセージコード
     * @param args 埋め込み引数
     * @return 解決済みメッセージ
     */
    private String message(String code, Object... args) {
        return messages.getMessage(code, args);
    }

    /**
     * 明細一覧から対象バリアントの位置を探す。
     *
     * @param items            カート明細
     * @param productVariantId 商品バリアントID
     * @return 見つかった位置。存在しない場合は {@code -1}
     */
    private int findItemIndex(List<CartCookieItem> items, long productVariantId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productVariantId() == productVariantId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 同一バリアントの重複明細を後勝ちで1件にまとめる。
     *
     * @param items カート明細
     * @return 重複除去後明細
     */
    private List<CartCookieItem> deduplicate(List<CartCookieItem> items) {
        Map<Long, CartCookieItem> map = new LinkedHashMap<>();
        for (CartCookieItem item : items) {
            map.put(item.productVariantId(), item);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 数量を画面仕様の範囲に正規化する。
     *
     * @param quantity 入力数量
     * @return 1〜99 に正規化した数量
     */
    private int normalizeQuantity(int quantity) {
        if (quantity < 1) {
            return 1;
        }
        return Math.min(quantity, MAX_QUANTITY_PER_LINE);
    }

    /**
     * 商品の組立可否を踏まえて組立指定を正規化する。
     *
     * @param requested         入力された組立指定
     * @param assemblyAvailable 組立対象商品か
     * @return 正規化後の組立指定。対象外商品の場合は {@code null}
     */
    private Boolean normalizeAssembly(Boolean requested, boolean assemblyAvailable) {
        if (!assemblyAvailable) {
            return null;
        }
        return Boolean.TRUE.equals(requested);
    }

    /**
     * 組立費を適用すべきか判定する。
     *
     * @param assemblyAvailable 組立対象商品か
     * @param assemblyRequested 組立指定ありか
     * @return 組立費適用有無
     */
    private boolean shouldApplyAssemblyFee(boolean assemblyAvailable, Boolean assemblyRequested) {
        return assemblyAvailable && Boolean.TRUE.equals(assemblyRequested);
    }

    /**
     * 在庫有無に応じた商品詳細URLを生成する。
     *
     * @param productId     商品ID
     * @param stockQuantity 在庫数
     * @return 商品詳細URL
     */
    private String buildProductDetailUrl(long productId, int stockQuantity) {
        return "/products/" + productId + (stockQuantity > 0 ? "" : "?stock=out");
    }

    /**
     * 同一リクエスト中は request attribute を優先して現在カートを読み出す。
     *
     * <p>
     * 1リクエスト内で複数回カート操作を行う際に、Cookie再読込で直前更新を失わないようにする。
     *
     * @param request 現在のHTTPリクエスト
     * @return 現在カート明細
     */
    @SuppressWarnings("unchecked")
    private List<CartCookieItem> loadCurrentCartItems(HttpServletRequest request) {
        if (request == null) {
            return List.of();
        }
        Object cached = request.getAttribute(REQUEST_ATTR_CART_ITEMS);
        if (cached instanceof List<?> cachedList) {
            return (List<CartCookieItem>) cachedList;
        }
        List<CartCookieItem> loaded = cartCookieStore.load(request);
        rememberCurrentCartItems(request, loaded);
        return loaded;
    }

    /**
     * カートCookie保存と request attribute 更新を同時に行う。
     *
     * @param request  現在のHTTPリクエスト
     * @param response 現在のHTTPレスポンス
     * @param items    保存対象明細
     */
    private void saveCurrentCartItems(HttpServletRequest request,
            HttpServletResponse response,
            List<CartCookieItem> items) {
        List<CartCookieItem> normalized = deduplicate(items);
        cartCookieStore.save(request, response, normalized);
        rememberCurrentCartItems(request, normalized);
    }

    /**
     * 同一リクエスト中に再利用するカート状態を request attribute に保持する。
     *
     * @param request 現在のHTTPリクエスト
     * @param items   保持対象明細
     */
    private void rememberCurrentCartItems(HttpServletRequest request, List<CartCookieItem> items) {
        if (request == null) {
            return;
        }
        request.setAttribute(REQUEST_ATTR_CART_ITEMS, List.copyOf(items));
    }

    /**
     * クーポン割引額を計算する
     * 
     * @param couponCode ユーザーが入力したクーポンコード
     * @param cart       カート表示情報
     */
    /**
     * クーポンを適用できるか検証する
     */
    public void applyCoupon(String couponCode, CartView cart, HttpServletRequest request,
            HttpServletResponse response) {

        if (couponCode == null || couponCode.isBlank()) {
            throw new IllegalArgumentException("クーポンコードを入力してください。");
        }

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("カートが空です。");
        }

        String normalizedCouponCode = couponCode.trim();

        // 1. DBから有効なクーポンを取得
        CouponForm coupon = orderRepository.findActiveCouponByCode(normalizedCouponCode)
                .orElseThrow(() -> new IllegalArgumentException("無効なクーポンコード、または期限切れです。"));

        // 2. カートの小計金額が最低購入金額を満たしているかチェック
        BigDecimal subtotal = calculateSubtotal(cart);

        if (coupon.minPurchaseAmount() != null && subtotal.compareTo(coupon.minPurchaseAmount()) < 0) {
            throw new IllegalArgumentException(
                    String.format("このクーポンは %,d 円以上のお買い上げでご利用いただけます。", coupon.minPurchaseAmount().longValue()));
        }

        BigDecimal discountAmount = calculateDiscountAmount(normalizedCouponCode, subtotal);

        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("このクーポンは現在ご利用いただけません");
        }

        Cookie cookie = new Cookie("APPLIED_COUPON", normalizedCouponCode);

        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
        // ※Cookie等へのクーポン保存処理は、プロジェクトの保持方法（Cookieストア等）に合わせて呼んでください
    }

    /**
     * クーポンコードとカート小計からクーポン割引額を計算する
     * 
     * @param couponCode クーポンコード
     * @param subtotal   カートの税抜小計(商品代金 + 組立費)
     * @return 割引額。クーポンが利用できない場合は{@link BigDecimal#ZERO}
     */
    public BigDecimal calculateDiscountAmount(String couponCode, BigDecimal subtotal) {
        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        String normalizedCouponCode = couponCode.trim();

        Optional<CouponForm> couponOpt = orderRepository.findActiveCouponByCode(normalizedCouponCode);
        if (couponOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        CouponForm coupon = couponOpt.get();

        // 最低購入金額に満たなくなっていれば割引適用外
        if (coupon.minPurchaseAmount() != null && subtotal.compareTo(coupon.minPurchaseAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        if (coupon.isFixed()) {
            // 定額割引
            BigDecimal discount = coupon.discountValue();
            return discount.compareTo(subtotal) > 0 ? subtotal : discount;
        }

        if (coupon.isPercentage()) {
            // 定率割引
            BigDecimal percentage = coupon.discountValue();
            return subtotal.multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        }

        return BigDecimal.ZERO;
    }

    /**
     * カートの小計金額（商品代金＋組立費）を計算する。
     *
     * @param cart カート表示情報
     * @return 商品代金と組立費を合算した税抜小計
     */
    private BigDecimal calculateSubtotal(CartView cart) {
        if (cart == null || cart.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cart.items().stream()
                .map(CartLineView::lineSubtotalBeforeTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * リクエストのCookieから適用中のクーポンコードを取得し、
     * 現在のカート小計に対する割引額を計算する
     * 
     * @param request  現在のHTTPリクエスト
     * @param subtotal 現在のカート税抜小計
     * @return 割引額。クーポンが存在しない、または利用できない場合は0
     */
    private BigDecimal getAppliedDiscountAmount(HttpServletRequest request, BigDecimal subtotal) {
        if (request == null || request.getCookies() == null) {
            return BigDecimal.ZERO;
        }

        // Cookieから "APPLIED_COUPON" などを探す
        String couponCode = Arrays.stream(request.getCookies())
                .filter(cookie -> "APPLIED_COUPON".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        // クーポンリポジトリ等から割引額を取得（存在しない/無効な場合は ZERO）
        // return
        // couponRepository.findDiscountAmountByCode(couponCode).orElse(BigDecimal.ZERO);

        // 例: 単純な固定値やサービス経由で取得する場合
        return calculateDiscountAmount(couponCode, subtotal);
    }

}
