package jp.co.skig.officeorder.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.common.MoneyFormatter;
import jp.co.skig.officeorder.mapper.row.ProductColorCodeMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductDetailMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductListMapperRow;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.mapper.row.ProductRankedMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductVariantMapperRow;
import jp.co.skig.officeorder.mapper.RangeValue;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductCategory;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSeriesLinkView;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.ProductVariantView;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import org.springframework.stereotype.Repository;

/**
 * 商品一覧・商品詳細表示に必要な取得と整形を担当するリポジトリ。
 *
 * <p>一覧検索条件をSQLパラメータへ変換し、Mapper行を画面表示用モデルへ整形する。
 */
@Repository
public class ProductRepository {

    /** デスク幅レンジIDと実寸範囲の対応表。 */
    private static final Map<Integer, RangeValue> DESK_WIDTH_RANGES = Map.ofEntries(
            Map.entry(1, new RangeValue(null, 999L)),
            Map.entry(2, new RangeValue(1000L, 1199L)),
            Map.entry(3, new RangeValue(1200L, 1399L)),
            Map.entry(4, new RangeValue(1400L, 1599L)),
            Map.entry(5, new RangeValue(1600L, 1799L)),
            Map.entry(6, new RangeValue(1800L, 1999L)),
            Map.entry(7, new RangeValue(2000L, 2399L)),
            Map.entry(8, new RangeValue(2400L, 2799L)),
            Map.entry(9, new RangeValue(2800L, 3199L)),
            Map.entry(10, new RangeValue(3200L, 3599L)),
            Map.entry(11, new RangeValue(3600L, null))
    );

    /** デスク奥行レンジIDと実寸範囲の対応表。 */
    private static final Map<Integer, RangeValue> DESK_DEPTH_RANGES = Map.ofEntries(
            Map.entry(1, new RangeValue(null, 499L)),
            Map.entry(2, new RangeValue(500L, 699L)),
            Map.entry(3, new RangeValue(700L, 899L)),
            Map.entry(4, new RangeValue(900L, 1099L)),
            Map.entry(5, new RangeValue(1100L, 1399L)),
            Map.entry(6, new RangeValue(1400L, null))
    );

    /** デスク高さレンジIDと実寸範囲の対応表。 */
    private static final Map<Integer, RangeValue> DESK_HEIGHT_RANGES = Map.ofEntries(
            Map.entry(1, new RangeValue(null, 699L)),
            Map.entry(2, new RangeValue(700L, 759L)),
            Map.entry(3, new RangeValue(760L, 799L)),
            Map.entry(4, new RangeValue(800L, null))
    );

    /** 商品SQLを呼び出す MyBatis Mapper。 */
    private final ProductMapper productMapper;
    /** 時刻依存条件に使う共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * 商品リポジトリを生成する。
     *
     * @param productMapper 商品Mapper
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public ProductRepository(ProductMapper productMapper, AppTimeProvider appTimeProvider) {
        this.productMapper = productMapper;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * 一覧検索条件で商品を検索し、ページ情報付きで返す。
     *
     * @param condition 検索条件
     * @return 一覧ページ情報
     */
    public ProductListPage search(ProductSearchCondition condition) {
        Map<String, Object> params = buildSearchParams(condition);
        long totalCount = Optional.ofNullable(productMapper.countProducts(params)).orElse(0L);
        if (totalCount == 0) {
            return new ProductListPage(List.of(), 0, condition.page(), condition.size());
        }

        params.put("limit", condition.size());
        params.put("offset", (long) (condition.page() - 1) * condition.size());
        List<ProductListMapperRow> rows = productMapper.selectProducts(params);

        Map<Long, List<String>> colorMap = findColorCodes(rows.stream().map(ProductListMapperRow::productId).toList());
        List<ProductCardView> items = rows.stream()
                .map(row -> toCard(row, colorMap.getOrDefault(row.productId(), List.of())))
                .toList();
        return new ProductListPage(items, totalCount, condition.page(), condition.size());
    }

    /**
     * 商品ID一覧から最近見た商品などの表示用商品を取得する。
     *
     * @param productIds 商品ID一覧
     * @param limit 取得件数上限
     * @return 表示用商品カード一覧
     */
    public List<ProductCardView> findByProductIds(List<Long> productIds, int limit) {
        if (productIds == null || productIds.isEmpty() || limit <= 0) {
            return List.of();
        }

        List<Long> normalizedIds = productIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(limit)
                .toList();
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        List<ProductListMapperRow> rows = productMapper.selectProductsByIds(
                normalizedIds,
                appTimeProvider.nowOffsetDateTime()
        );
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductListMapperRow> rowsByProductId = new HashMap<>();
        for (ProductListMapperRow row : rows) {
            rowsByProductId.put(row.productId(), row);
        }

        Map<Long, List<String>> colorMap = findColorCodes(rows.stream().map(ProductListMapperRow::productId).toList());
        List<ProductCardView> result = new ArrayList<>();
        for (Long productId : normalizedIds) {
            ProductListMapperRow row = rowsByProductId.get(productId);
            if (row == null) {
                continue;
            }
            result.add(toCard(row, colorMap.getOrDefault(row.productId(), List.of())));
        }
        return result;
    }

    /**
     * トップ・新着一覧向けの新着商品を取得する。
     *
     * @param limit 取得件数上限
     * @return 新着商品一覧
     */
    public List<ProductCardView> findNewestProducts(int limit) {
        ProductSearchCondition condition = new ProductSearchCondition(
                null,
                null,
                false,
                List.of(),
                List.of(),
                ProductCategoryFilter.empty(),
                ProductSort.NEWEST,
                1,
                limit,
                appTimeProvider.nowOffsetDateTime().minusMonths(6)
        );
        return search(condition).items();
    }

    /**
     * トップ画面用の売れ筋ランキング商品を取得する。
     *
     * <p>ランキングテーブルが空の場合は新着商品で代替する。
     *
     * @param limit 取得件数上限
     * @return 順位付き商品カード一覧
     */
    public List<RankedProductCardView> findTopRankedProducts(int limit) {
        List<ProductRankedMapperRow> rows = productMapper.selectTopRankedProducts(limit);
        if (rows.isEmpty()) {
            List<ProductCardView> fallback = findNewestProducts(limit);
            List<RankedProductCardView> fallbackRanked = new ArrayList<>();
            for (int i = 0; i < fallback.size(); i++) {
                fallbackRanked.add(new RankedProductCardView(i + 1, fallback.get(i)));
            }
            return fallbackRanked;
        }

        Map<Long, List<String>> colorMap = findColorCodes(rows.stream().map(ProductRankedMapperRow::productId).toList());
        return rows.stream()
                .map(row -> new RankedProductCardView(
                        row.rank(),
                        toCard(
                                toListRow(row),
                                colorMap.getOrDefault(row.productId(), List.of())
                        )
                ))
                .toList();
    }

    /**
     * 商品詳細画面表示用の情報を取得する。
     *
     * @param productId 商品ID
     * @param forceOutOfStock 在庫切れ表示を強制するか
     * @return 商品詳細
     */
    public Optional<ProductDetailView> findDetail(long productId, boolean forceOutOfStock) {
        OffsetDateTime now = appTimeProvider.nowOffsetDateTime();
        ProductDetailMapperRow product = productMapper.selectProductDetail(productId, now);
        if (product == null) {
            return Optional.empty();
        }

        List<ProductVariantView> variants = productMapper.selectProductVariants(productId).stream()
                .map(this::toProductVariantView)
                .toList();
        if (variants.isEmpty()) {
            return Optional.empty();
        }

        ProductVariantView selectedVariant = selectVariant(variants, forceOutOfStock);
        BigDecimal taxRate = findCurrentTaxRatePercent(now);
        BigDecimal priceIncludingTax = selectedVariant.unitPrice()
                .multiply(BigDecimal.ONE.add(taxRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.DOWN);
        BigDecimal assemblyFee = product.assemblyAvailable()
                ? product.assemblyFee()
                : BigDecimal.ZERO;

        List<ProductSeriesLinkView> seriesLinks = findSeriesLinks(product.productId(), product.variationGroupId(), now);
        List<ProductCardView> relatedProducts = findRecommendedProducts(product.productId(), 4);

        ProductDetailView detail = new ProductDetailView(
                product.productId(),
                product.productName(),
                product.description() == null ? "" : product.description(),
                product.categoryId(),
                product.hasVariation(),
                product.variationGroupId(),
                product.variationName(),
                taxRate,
                selectedVariant.unitPrice(),
                MoneyFormatter.formatYen(selectedVariant.unitPrice()),
                priceIncludingTax,
                MoneyFormatter.formatYen(priceIncludingTax),
                assemblyFee,
                MoneyFormatter.formatYen(assemblyFee),
                selectedVariant,
                variants,
                seriesLinks,
                relatedProducts,
                selectedVariant.stockQuantity() <= 0 || forceOutOfStock
        );
        return Optional.of(detail);
    }

    /**
     * 検索条件をMyBatis検索パラメータへ変換する。
     *
     * <p>カテゴリ固有フィルタの有無や価格・寸法レンジをここでSQL向けに解決する。
     *
     * @param condition 検索条件
     * @return SQLパラメータ
     */
    private Map<String, Object> buildSearchParams(ProductSearchCondition condition) {
        Map<String, Object> params = new HashMap<>();
        ProductCategoryFilter filter = condition.categoryFilter() == null
                ? ProductCategoryFilter.empty()
                : condition.categoryFilter();
        ProductCategory category = ProductCategory.fromId(condition.categoryId()).orElse(null);
        String normalizedCategoryId = category == null ? condition.categoryId() : category.id();

        List<Long> colorIds = condition.colorIds() == null ? List.of() : condition.colorIds();
        List<RangeValue> priceRanges = toPriceRanges(condition.priceBands());
        List<Integer> deskTopShapeIds = filter.deskTopShapeIds();
        List<Integer> deskTasteIds = filter.deskTasteIds();
        List<RangeValue> deskWidthRanges = toRanges(filter.deskWidthBandIds(), DESK_WIDTH_RANGES);
        List<RangeValue> deskDepthRanges = toRanges(filter.deskDepthBandIds(), DESK_DEPTH_RANGES);
        List<RangeValue> deskHeightRanges = toRanges(filter.deskHeightBandIds(), DESK_HEIGHT_RANGES);
        List<Integer> chairFunctionIds = filter.chairFunctionIds();
        List<Integer> chairMaterialIds = filter.chairMaterialIds();
        List<Integer> chairTasteIds = filter.chairTasteIds();
        List<Integer> storageUsageIds = filter.storageUsageIds();
        List<Integer> storageTasteIds = filter.storageTasteIds();
        List<String> keywordWords = normalizeAndSplitKeywordWords(condition.keyword());

        boolean hasVariantFilter = condition.inStockOnly()
                || !colorIds.isEmpty()
                || !priceRanges.isEmpty();
        boolean hasDeskFilter = category == ProductCategory.DESK && (!deskTopShapeIds.isEmpty()
                || !deskTasteIds.isEmpty()
                || !deskWidthRanges.isEmpty()
                || !deskDepthRanges.isEmpty()
                || !deskHeightRanges.isEmpty());
        boolean hasChairFilter = category == ProductCategory.CHAIR && (!chairFunctionIds.isEmpty()
                || !chairMaterialIds.isEmpty()
                || !chairTasteIds.isEmpty());
        boolean hasStorageFilter = category == ProductCategory.STORAGE && (!storageUsageIds.isEmpty()
                || !storageTasteIds.isEmpty());
        boolean hasSearchTasteFilter = !deskTasteIds.isEmpty()
                || !chairTasteIds.isEmpty()
                || !storageTasteIds.isEmpty();

        params.put("categoryId", normalizedCategoryId);
        params.put("keywordWords", keywordWords);
        params.put("inStockOnly", condition.inStockOnly());
        params.put("colorIds", colorIds);
        params.put("priceRanges", priceRanges);
        params.put("sort", condition.sort() == null ? ProductSort.RECOMMENDED.value() : condition.sort().value());
        params.put("saleStartFrom", condition.saleStartFrom());
        params.put("hasVariantFilter", hasVariantFilter);
        params.put("hasDeskFilter", hasDeskFilter);
        params.put("hasChairFilter", hasChairFilter);
        params.put("hasStorageFilter", hasStorageFilter);
        params.put("hasSearchTasteFilter", hasSearchTasteFilter);
        params.put("now", appTimeProvider.nowOffsetDateTime());

        params.put("deskTopShapeIds", deskTopShapeIds);
        params.put("deskTasteIds", deskTasteIds);
        params.put("deskWidthRanges", deskWidthRanges);
        params.put("deskDepthRanges", deskDepthRanges);
        params.put("deskHeightRanges", deskHeightRanges);
        params.put("chairFunctionIds", chairFunctionIds);
        params.put("chairMaterialIds", chairMaterialIds);
        params.put("chairTasteIds", chairTasteIds);
        params.put("storageUsageIds", storageUsageIds);
        params.put("storageTasteIds", storageTasteIds);
        return params;
    }

    /**
     * 価格帯定義を RangeValue の一覧へ変換する。
     *
     * @param priceBands 価格帯定義
     * @return SQL向け価格レンジ一覧
     */
    private List<RangeValue> toPriceRanges(List<PriceBand> priceBands) {
        if (priceBands == null || priceBands.isEmpty()) {
            return List.of();
        }
        return priceBands.stream()
                .map(band -> new RangeValue(band.min(), band.max()))
                .distinct()
                .toList();
    }

    /**
     * レンジID一覧を対応表から実寸レンジへ変換する。
     *
     * @param bandIds レンジID一覧
     * @param definitions レンジ定義
     * @return 実寸レンジ一覧
     */
    private List<RangeValue> toRanges(List<Integer> bandIds, Map<Integer, RangeValue> definitions) {
        if (bandIds == null || bandIds.isEmpty()) {
            return List.of();
        }
        return bandIds.stream()
                .map(definitions::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> normalizeAndSplitKeywordWords(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalizedKeyword = keyword.replace('\u3000', ' ').trim();
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }
        return List.of(normalizedKeyword.split("\\s+"))
                .stream()
                .map(this::normalizeKeywordToken)
                .filter(token -> token != null && !token.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    private String normalizeKeywordToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return Normalizer.normalize(token.trim(), Normalizer.Form.NFKC);
    }

    /**
     * 一覧カード表示用のカラーコードを商品単位で取得する。
     *
     * @param productIds 商品ID一覧
     * @return 商品IDごとのカラーコード一覧
     */
    private Map<Long, List<String>> findColorCodes(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductColorCodeMapperRow> rows = productMapper.selectColorCodes(productIds);
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (ProductColorCodeMapperRow row : rows) {
            result.computeIfAbsent(row.productId(), ignored -> new ArrayList<>()).add(row.colorCode());
        }
        return result;
    }

    /**
     * 同一シリーズバリエーショングループの商品リンクを取得する。
     *
     * @param productId 現在表示中の商品ID
     * @param variationGroupId バリエーショングループID
     * @param now 販売期間判定時刻
     * @return シリーズリンク一覧
     */
    private List<ProductSeriesLinkView> findSeriesLinks(long productId,
                                                        Long variationGroupId,
                                                        OffsetDateTime now) {
        if (variationGroupId == null) {
            return List.of();
        }
        List<ProductSeriesLinkView> links = productMapper.selectSeriesLinks(variationGroupId, now).stream()
                .map(row -> new ProductSeriesLinkView(
                        row.productId(),
                        row.productName(),
                        row.productId() == productId
                ))
                .toList();
        return links;
    }

    /**
     * 商品詳細下部に表示するおすすめ関連商品を取得する。
     *
     * <p>ランキングテーブルが空の場合は新着商品から代替候補を返す。
     *
     * @param sourceProductId 元商品ID
     * @param limit 取得件数上限
     * @return 商品カード一覧
     */
    private List<ProductCardView> findRecommendedProducts(long sourceProductId, int limit) {
        List<ProductRankedMapperRow> rankedRows = productMapper.selectRecommendedProducts(sourceProductId, limit);
        if (rankedRows.isEmpty()) {
            ProductSearchCondition fallbackCondition = new ProductSearchCondition(
                    null,
                    null,
                    false,
                    List.of(),
                    List.of(),
                    ProductCategoryFilter.empty(),
                    ProductSort.NEWEST,
                    1,
                    limit + 1,
                    null
            );
            return search(fallbackCondition).items().stream()
                    .filter(item -> item.productId() != sourceProductId)
                    .limit(limit)
                    .toList();
        }
        List<Long> ids = rankedRows.stream().map(ProductRankedMapperRow::productId).toList();
        Map<Long, List<String>> colorMap = findColorCodes(ids);
        return rankedRows.stream()
                .sorted(Comparator.comparingInt(ProductRankedMapperRow::rank))
                .map(row -> toCard(
                        toListRow(row),
                        colorMap.getOrDefault(row.productId(), List.of())
                ))
                .toList();
    }

    /**
     * 商品詳細で初期選択するカラーを決定する。
     *
     * <p>在庫切れ表示強制時は在庫なしカラーを優先し、通常時は在庫ありカラーを優先する。
     *
     * @param variants カラー候補一覧
     * @param forceOutOfStock 在庫切れ表示を強制するか
     * @return 初期選択カラー
     */
    private ProductVariantView selectVariant(List<ProductVariantView> variants, boolean forceOutOfStock) {
        if (forceOutOfStock) {
            Optional<ProductVariantView> out = variants.stream()
                    .filter(v -> v.stockQuantity() <= 0)
                    .findFirst();
            if (out.isPresent()) {
                return out.get();
            }
        }
        return variants.stream()
                .filter(v -> v.stockQuantity() > 0)
                .findFirst()
                .orElse(variants.getFirst());
    }

    /**
     * 指定時点で有効な消費税率を取得する。
     *
     * @param now 判定時刻
     * @return 税率百分率。未設定時は 10
     */
    private BigDecimal findCurrentTaxRatePercent(OffsetDateTime now) {
        BigDecimal taxRate = productMapper.selectCurrentTaxRatePercent(now);
        return taxRate == null ? BigDecimal.TEN : taxRate;
    }

    /**
     * 一覧行を商品カード表示用モデルへ変換する。
     *
     * @param row 一覧行
     * @param colors カラーコード一覧
     * @return 商品カード
     */
    private ProductCardView toCard(ProductListMapperRow row, List<String> colors) {
        boolean inStock = row.maxStock() > 0;
        String detailUrl = "/products/" + row.productId() + (inStock ? "" : "?stock=out");
        return new ProductCardView(
                row.productId(),
                row.productName(),
                MoneyFormatter.formatYen(row.minPrice()),
                colors,
                row.productCode(),
                inStock,
                detailUrl
        );
    }

    /**
     * ランキング行を一覧行互換モデルへ変換する。
     *
     * @param row ランキング行
     * @return 一覧行
     */
    private ProductListMapperRow toListRow(ProductRankedMapperRow row) {
        return new ProductListMapperRow(
                row.productId(),
                row.productName(),
                row.minPrice(),
                row.maxStock(),
                row.productCode()
        );
    }

    /**
     * 商品バリアント行を詳細画面用モデルへ変換する。
     *
     * @param row バリアント行
     * @return 商品バリアント表示モデル
     */
    private ProductVariantView toProductVariantView(ProductVariantMapperRow row) {
        BigDecimal unitPrice = row.unitPrice();
        return new ProductVariantView(
                row.productVariantId(),
                row.productCode(),
                row.colorId(),
                row.colorName(),
                row.colorCode(),
                unitPrice,
                MoneyFormatter.formatYen(unitPrice),
                row.stockQuantity()
        );
    }
}





