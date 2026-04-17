package jp.co.skig.officeorder.web;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategory;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.service.member.FavoritesLimitExceededException;
import jp.co.skig.officeorder.service.member.MemberService;
import jp.co.skig.officeorder.service.product.ProductFilterOptionService;
import jp.co.skig.officeorder.service.product.ProductListSearchService;
import jp.co.skig.officeorder.service.product.ProductService;
import jp.co.skig.officeorder.web.auth.MemberSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

/**
 * 商品一覧・商品詳細・お気に入り操作を担当するController。
 */
@Controller
public class CatalogController {

    /** 商品画面ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);
    /** 最近見た商品として扱う最大件数。 */
    private static final int RECENTLY_VIEWED_LIMIT = 4;

    /** 商品検索・詳細サービス。 */
    private final ProductService productService;
    /** 一覧検索条件組み立てサービス。 */
    private final ProductListSearchService productListSearchService;
    /** 絞り込み候補サービス。 */
    private final ProductFilterOptionService productFilterOptionService;
    /** 会員サービス。 */
    private final MemberService memberService;
    /** 会員セッションサービス。 */
    private final MemberSessionService memberSessionService;

    /**
     * 商品Catalog Controllerを生成する。
     *
     * @param productService 商品サービス
     * @param productListSearchService 一覧検索サービス
     * @param productFilterOptionService 絞り込み候補サービス
     * @param memberService 会員サービス
     * @param memberSessionService 会員セッションサービス
     */
    public CatalogController(ProductService productService,
                             ProductListSearchService productListSearchService,
                             ProductFilterOptionService productFilterOptionService,
                             MemberService memberService,
                             MemberSessionService memberSessionService) {
        this.productService = productService;
        this.productListSearchService = productListSearchService;
        this.productFilterOptionService = productFilterOptionService;
        this.memberService = memberService;
        this.memberSessionService = memberSessionService;
    }

    /**
     * 新着商品一覧を表示する。
     */
    @GetMapping("/products/new-arrivals")
    public String newArrivals(@RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
                              @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
                              @RequestParam(name = "color", required = false) List<String> rawColorKeys,
                              @RequestParam(name = "sort", required = false) String sort,
                              @RequestParam(name = "page", defaultValue = "1") int page,
                              @RequestParam(name = "size", defaultValue = "15") int size,
                              Model model) {
        ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
        ProductSearchCondition condition = productListSearchService.buildNewArrivalCondition(
                inStockOnly,
                rawPriceBandIds,
                rawColorKeys,
                sort,
                page,
                size,
                optionsBundle
        );
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);
        applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);
        return "pages/product-list-new-arrivals";
    }

    /**
     * キーワード検索結果一覧を表示する。
     */
    @GetMapping("/products/search")
    public String searchResults(@RequestParam(name = "q", required = false) String keyword,
                                @RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
                                @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
                                @RequestParam(name = "color", required = false) List<String> rawColorKeys,
                                @RequestParam(name = "taste", required = false) List<String> rawTasteDisplayNames,
                                @RequestParam(name = "sort", required = false) String sort,
                                @RequestParam(name = "page", defaultValue = "1") int page,
                                @RequestParam(name = "size", defaultValue = "15") int size,
                                Model model) {
        ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
        List<String> tasteFilterOptions = productFilterOptionService.loadUnifiedTasteDisplayNames();
        ProductSearchCondition condition = productListSearchService.buildCondition(
                null,
                keyword,
                inStockOnly,
                rawPriceBandIds,
                rawColorKeys,
                sort,
                page,
                size,
                ProductSort.RECOMMENDED,
                optionsBundle,
                rawTasteDisplayNames
        );
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);
        applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);
        model.addAttribute("keyword", result.condition().keyword() == null ? "" : result.condition().keyword());
        model.addAttribute("tasteFilterOptions", tasteFilterOptions);
        model.addAttribute("selectedTasteDisplayNames", result.condition().tasteDisplayNames());
        return "pages/product-list-search-results";
    }

    /**
     * デスクカテゴリ一覧を表示する。
     */
    @GetMapping("/categories/desks")
    public String desks(@RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
                        @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
                        @RequestParam(name = "color", required = false) List<String> rawColorKeys,
                        @RequestParam(name = "deskTopShape", required = false) List<Integer> rawDeskTopShapeIds,
                        @RequestParam(name = "deskWidthBand", required = false) List<Integer> rawDeskWidthBandIds,
                        @RequestParam(name = "deskDepthBand", required = false) List<Integer> rawDeskDepthBandIds,
                        @RequestParam(name = "deskHeightBand", required = false) List<Integer> rawDeskHeightBandIds,
                        @RequestParam(name = "deskTaste", required = false) List<Integer> rawDeskTasteIds,
                        @RequestParam(name = "sort", required = false) String sort,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "15") int size,
                        Model model) {
        ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
        ProductCategoryFilter deskFilter = productFilterOptionService.buildDeskFilter(
                rawDeskTopShapeIds,
                rawDeskWidthBandIds,
                rawDeskDepthBandIds,
                rawDeskHeightBandIds,
                rawDeskTasteIds,
                optionsBundle
        );
        ProductSearchCondition condition = productListSearchService.buildCondition(
                ProductCategory.DESK.id(),
                null,
                inStockOnly,
                rawPriceBandIds,
                rawColorKeys,
                deskFilter,
                sort,
                page,
                size,
                ProductSort.RECOMMENDED,
                optionsBundle
        );
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);
        applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);
        model.addAttribute("deskTopShapeIds", deskFilter.deskTopShapeIds());
        model.addAttribute("deskWidthBandIds", deskFilter.deskWidthBandIds());
        model.addAttribute("deskDepthBandIds", deskFilter.deskDepthBandIds());
        model.addAttribute("deskHeightBandIds", deskFilter.deskHeightBandIds());
        model.addAttribute("deskTasteIds", deskFilter.deskTasteIds());
        model.addAttribute("deskTopShapeOptions", optionsBundle.deskTopShapeOptions());
        model.addAttribute("deskTasteOptions", optionsBundle.deskTasteOptions());
        return "pages/product-list-category-desk";
    }

    /**
     * チェアカテゴリ一覧を表示する。
     */
    @GetMapping("/categories/chairs")
    public String chairs(@RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
                         @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
                         @RequestParam(name = "color", required = false) List<String> rawColorKeys,
                         @RequestParam(name = "chairFunction", required = false) List<Integer> rawChairFunctionIds,
                         @RequestParam(name = "chairMaterial", required = false) List<Integer> rawChairMaterialIds,
                         @RequestParam(name = "chairTaste", required = false) List<Integer> rawChairTasteIds,
                         @RequestParam(name = "sort", required = false) String sort,
                         @RequestParam(name = "page", defaultValue = "1") int page,
                         @RequestParam(name = "size", defaultValue = "15") int size,
                         Model model) {
        ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
        ProductCategoryFilter chairFilter = productFilterOptionService.buildChairFilter(
                rawChairFunctionIds,
                rawChairMaterialIds,
                rawChairTasteIds,
                optionsBundle
        );
        ProductSearchCondition condition = productListSearchService.buildCondition(
                ProductCategory.CHAIR.id(),
                null,
                inStockOnly,
                rawPriceBandIds,
                rawColorKeys,
                chairFilter,
                sort,
                page,
                size,
                ProductSort.RECOMMENDED,
                optionsBundle
        );
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);
        applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);
        model.addAttribute("chairFunctionIds", chairFilter.chairFunctionIds());
        model.addAttribute("chairMaterialIds", chairFilter.chairMaterialIds());
        model.addAttribute("chairTasteIds", chairFilter.chairTasteIds());
        model.addAttribute("chairFunctionOptions", optionsBundle.chairFunctionOptions());
        model.addAttribute("chairMaterialOptions", optionsBundle.chairMaterialOptions());
        model.addAttribute("chairTasteOptions", optionsBundle.chairTasteOptions());
        return "pages/product-list-category-chair";
    }

    /**
     * 収納家具カテゴリ一覧を表示する。
     */
    @GetMapping("/categories/storages")
    public String storages(@RequestParam(name = "inStockOnly", defaultValue = "false") boolean inStockOnly,
                           @RequestParam(name = "priceBand", required = false) List<Integer> rawPriceBandIds,
                           @RequestParam(name = "color", required = false) List<String> rawColorKeys,
                           @RequestParam(name = "storageUsage", required = false) List<Integer> rawStorageUsageIds,
                           @RequestParam(name = "storageTaste", required = false) List<Integer> rawStorageTasteIds,
                           @RequestParam(name = "sort", required = false) String sort,
                           @RequestParam(name = "page", defaultValue = "1") int page,
                           @RequestParam(name = "size", defaultValue = "15") int size,
                           Model model) {
        ProductFilterOptionsBundle optionsBundle = productFilterOptionService.loadOptionsBundle();
        ProductCategoryFilter storageFilter = productFilterOptionService.buildStorageFilter(
                rawStorageUsageIds,
                rawStorageTasteIds,
                optionsBundle
        );
        ProductSearchCondition condition = productListSearchService.buildCondition(
                ProductCategory.STORAGE.id(),
                null,
                inStockOnly,
                rawPriceBandIds,
                rawColorKeys,
                storageFilter,
                sort,
                page,
                size,
                ProductSort.RECOMMENDED,
                optionsBundle
        );
        ProductListSearchResult result = productListSearchService.searchWithPageCorrection(condition);
        applyProductListModel(model, result.condition(), result.productPage(), optionsBundle);
        model.addAttribute("storageUsageIds", storageFilter.storageUsageIds());
        model.addAttribute("storageTasteIds", storageFilter.storageTasteIds());
        model.addAttribute("storageUsageOptions", optionsBundle.storageUsageOptions());
        model.addAttribute("storageTasteOptions", optionsBundle.storageTasteOptions());
        return "pages/product-list-category-storage";
    }

    /**
     * 商品詳細画面を表示する。
     */
    @GetMapping("/products/{productId}")
    public String productDetail(@PathVariable long productId,
                                @RequestParam(name = "stock", required = false) String stock,
                                HttpSession session,
                                Model model) {
        boolean forceOutOfStock = "out".equalsIgnoreCase(stock);
        ProductDetailView detail = productService.findDetail(productId, forceOutOfStock)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        boolean isFavorite = memberSessionService.currentMember(session)
                .map(member -> memberService.isFavorite(member.memberId(), productId))
                .orElse(false);

        model.addAttribute("detail", detail);
        model.addAttribute("categoryLabel", resolveCategoryLabel(detail.categoryId()));
        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("detailPagePath", buildProductDetailPath(productId, forceOutOfStock));
        return "pages/product-detail";
    }

    /**
     * 最近見た商品一覧をJSONで返す。
     */
    @GetMapping("/products/recently-viewed")
    @ResponseBody
    public List<ProductCardView> recentlyViewed(@RequestParam(name = "ids", required = false) String ids) {
        List<Long> productIds = parseRecentlyViewedIds(ids, RECENTLY_VIEWED_LIMIT);
        if (productIds.isEmpty()) {
            return List.of();
        }
        return productService.findRecentlyViewedProducts(productIds, RECENTLY_VIEWED_LIMIT);
    }

    /**
     * 商品詳細などからのお気に入り追加・解除を受け付ける。
     */
    @PostMapping("/products/{productId}/favorite")
    public String toggleFavorite(@PathVariable long productId,
                                 @RequestParam(name = "stock", required = false) String stock,
                                 @RequestParam(name = "redirect", required = false) String redirect,
                                 @RequestParam(name = "action", defaultValue = "toggle") String action,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        boolean forceOutOfStock = "out".equalsIgnoreCase(stock);
        String defaultPath = buildProductDetailPath(productId, forceOutOfStock);
        String redirectPath = memberSessionService.sanitizeRedirectPath(redirect);
        if (redirectPath == null) {
            redirectPath = defaultPath;
        }

        productService.findDetail(productId, forceOutOfStock)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        MemberSessionUser member = memberSessionService.currentMember(session).orElse(null);
        if (member == null) {
            log.info("event={} productId={} reason=not_authenticated",
                    LogEvent.FAVORITE_REDIRECT_LOGIN.value(),
                    productId);
            String encoded = UriUtils.encodeQueryParam(redirectPath, StandardCharsets.UTF_8);
            return "redirect:/login?redirect=" + encoded;
        }

        if ("remove".equalsIgnoreCase(action)) {
            memberService.removeFavorite(member.memberId(), productId);
            log.info("event={} memberId={} productId={} action=remove",
                    LogEvent.FAVORITE_ACTION_ACCEPTED.value(),
                    member.memberId(),
                    productId);
            return "redirect:" + redirectPath;
        }

        try {
            memberService.toggleFavorite(member.memberId(), productId);
            log.info("event={} memberId={} productId={} action={}",
                    LogEvent.FAVORITE_ACTION_ACCEPTED.value(),
                    member.memberId(),
                    productId,
                    action);
        } catch (FavoritesLimitExceededException ex) {
            log.warn("event={} memberId={} productId={} reason=limit_exceeded",
                    LogEvent.FAVORITE_ACTION_REJECTED.value(),
                    member.memberId(),
                    productId);
            redirectAttributes.addFlashAttribute("favoriteLimitError", ex.getMessage());
        }
        return "redirect:" + redirectPath;
    }

    /**
     * 商品一覧画面共通で使うモデル属性を設定する。
     */
    private void applyProductListModel(Model model,
                                       ProductSearchCondition condition,
                                       ProductListPage productPage,
                                       ProductFilterOptionsBundle optionsBundle) {
        model.addAttribute("products", productPage.items());
        model.addAttribute("totalCount", productPage.totalCount());
        model.addAttribute("currentPage", condition.page());
        model.addAttribute("pageSize", condition.size());
        model.addAttribute("sortValue", condition.sort().value());
        model.addAttribute("inStockOnly", condition.inStockOnly());
        model.addAttribute("selectedPriceBandIds", condition.priceBands().stream().map(PriceBand::id).toList());
        model.addAttribute("selectedColorKeys", productFilterOptionService.resolveSelectedColorKeys(condition.colorIds(), optionsBundle));
        model.addAttribute("colorFilterOptions", optionsBundle.colorOptions());
        model.addAttribute("totalPages", productListSearchService.calculateTotalPages(productPage.totalCount(), condition.size()));
    }

    /**
     * カテゴリIDからパンくず用表示名を返す。
     */
    private String resolveCategoryLabel(String categoryId) {
        return ProductCategory.fromId(categoryId)
                .map(ProductCategory::label)
                .orElse("商品");
    }

    /**
     * 在庫切れ強制表示を含めた商品詳細URLを生成する。
     */
    private String buildProductDetailPath(long productId, boolean forceOutOfStock) {
        return "/products/" + productId + (forceOutOfStock ? "?stock=out" : "");
    }

    /**
     * ローカルストレージの最近見た商品ID文字列を安全にパースする。
     */
    private List<Long> parseRecentlyViewedIds(String rawIds, int limit) {
        if (rawIds == null || rawIds.isBlank() || limit <= 0) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String token : rawIds.split(",")) {
            if (result.size() >= limit) {
                break;
            }
            if (token == null) {
                continue;
            }
            String normalized = token.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                long id = Long.parseLong(normalized);
                if (id <= 0 || result.contains(id)) {
                    continue;
                }
                result.add(id);
            } catch (NumberFormatException ignored) {
                // ignore malformed token
            }
        }
        return result;
    }
}
