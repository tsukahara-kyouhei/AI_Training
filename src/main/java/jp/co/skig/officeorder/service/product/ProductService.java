package jp.co.skig.officeorder.service.product;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCardView;
import jp.co.skig.officeorder.model.product.ProductDetailView;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.model.product.RankedProductCardView;
import jp.co.skig.officeorder.repository.ProductRepository;
import org.springframework.stereotype.Service;

/**
 * 商品表示系ユースケースの窓口となるサービス。
 *
 * <p>トップ画面、新着一覧、カテゴリ一覧、検索結果、商品詳細で使う
 * 商品取得と検索条件正規化を集約している。
 */
@Service
public class ProductService {

    /** 一覧画面で許可する表示件数。 */
    private static final int[] ALLOWED_PAGE_SIZES = {15, 30, 60};
    /** トップ画面の売れ筋ランキング表示件数。 */
    private static final int TOP_RANKED_LIMIT = 8;

    /** 商品参照を担当するリポジトリ。 */
    private final ProductRepository repository;
    /** 新着判定などの基準時刻を返す共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * 商品サービスを生成する。
     *
     * @param repository 商品参照リポジトリ
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public ProductService(ProductRepository repository, AppTimeProvider appTimeProvider) {
        this.repository = repository;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * トップ画面用の新着商品を取得する。
     *
     * @return 新着商品4件
     */
    public List<ProductCardView> findTopNewArrivals() {
        return repository.findNewestProducts(4);
    }

    /**
     * トップ画面用の売れ筋ランキング商品を取得する。
     *
     * @return 売れ筋ランキング上位商品
     */
    public List<RankedProductCardView> findTopRankedProducts() {
        return repository.findTopRankedProducts(TOP_RANKED_LIMIT);
    }

    /**
     * 商品一覧条件を正規化して検索する。
     *
     * @param condition 検索条件
     * @return 一覧ページ情報
     */
    public ProductListPage search(ProductSearchCondition condition) {
        return repository.search(normalize(condition));
    }

    /**
     * 最近見た商品ID一覧から表示用商品を取得する。
     *
     * @param productIds 最近見た商品ID一覧
     * @param limit 取得件数上限
     * @return 商品カード一覧
     */
    public List<ProductCardView> findRecentlyViewedProducts(List<Long> productIds, int limit) {
        return repository.findByProductIds(productIds, limit);
    }

    /**
     * 商品詳細を取得する。
     *
     * @param productId 商品ID
     * @param forceOutOfStock 在庫切れ表示を強制するか
     * @return 商品詳細
     */
    public Optional<ProductDetailView> findDetail(long productId, boolean forceOutOfStock) {
        return repository.findDetail(productId, forceOutOfStock);
    }

    /**
     * 新着商品一覧向けの検索条件を組み立てる。
     *
     * <p>新着は販売開始日が直近6か月以内の商品に限定する。
     *
     * @param inStockOnly 在庫ありのみ条件
     * @param priceBandIds 価格帯ID一覧
     * @param colorIds 色ID一覧
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildNewArrivalCondition(boolean inStockOnly,
                                                           List<Integer> priceBandIds,
                                                           List<Long> colorIds,
                                                           String sort,
                                                           int page,
                                                           int size) {
        return buildCondition(
                null,
                null,
                inStockOnly,
                priceBandIds,
                colorIds,
                sort,
                page,
                size,
                ProductSort.NEWEST,
                ProductCategoryFilter.empty(),
                newArrivalSaleStartFrom()
        );
    }

    /**
     * カテゴリ・検索一覧向けの検索条件を組み立てる。
     *
     * @param categoryId カテゴリID
     * @param keyword キーワード
     * @param inStockOnly 在庫ありのみ条件
     * @param priceBandIds 価格帯ID一覧
     * @param colorIds 色ID一覧
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @param defaultSort デフォルト並び順
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildCondition(String categoryId,
                                                 String keyword,
                                                 boolean inStockOnly,
                                                 List<Integer> priceBandIds,
                                                 List<Long> colorIds,
                                                 String sort,
                                                 int page,
                                                 int size,
                                                 ProductSort defaultSort) {
        return buildCondition(
                categoryId,
                keyword,
                inStockOnly,
                priceBandIds,
                colorIds,
                sort,
                page,
                size,
                defaultSort,
                ProductCategoryFilter.empty(),
                null
        );
    }

    /**
     * カテゴリ固有条件付き一覧向けの検索条件を組み立てる。
     *
     * @param categoryId カテゴリID
     * @param keyword キーワード
     * @param inStockOnly 在庫ありのみ条件
     * @param priceBandIds 価格帯ID一覧
     * @param colorIds 色ID一覧
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @param defaultSort デフォルト並び順
     * @param categoryFilter カテゴリ固有条件
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildCondition(String categoryId,
                                                 String keyword,
                                                 boolean inStockOnly,
                                                 List<Integer> priceBandIds,
                                                 List<Long> colorIds,
                                                 String sort,
                                                 int page,
                                                 int size,
                                                 ProductSort defaultSort,
                                                 ProductCategoryFilter categoryFilter) {
        return buildCondition(
                categoryId,
                keyword,
                inStockOnly,
                priceBandIds,
                colorIds,
                sort,
                page,
                size,
                defaultSort,
                categoryFilter,
                null
        );
    }

    /**
     * 全条件を受け取り、一覧検索用の条件オブジェクトを構築する。
     *
     * @param categoryId カテゴリID
     * @param keyword キーワード
     * @param inStockOnly 在庫ありのみ条件
     * @param priceBandIds 価格帯ID一覧
     * @param colorIds 色ID一覧
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @param defaultSort デフォルト並び順
     * @param categoryFilter カテゴリ固有条件
     * @param saleStartFrom 販売開始日時の下限
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildCondition(String categoryId,
                                                 String keyword,
                                                 boolean inStockOnly,
                                                 List<Integer> priceBandIds,
                                                 List<Long> colorIds,
                                                 String sort,
                                                 int page,
                                                 int size,
                                                 ProductSort defaultSort,
                                                 ProductCategoryFilter categoryFilter,
                                                 OffsetDateTime saleStartFrom) {
        List<PriceBand> bands = new ArrayList<>();
        if (priceBandIds != null) {
            for (Integer id : priceBandIds) {
                if (id == null) {
                    continue;
                }
                PriceBand band = PriceBand.fromId(id);
                if (band != null && !bands.contains(band)) {
                    bands.add(band);
                }
            }
        }
        List<Long> uniqueColorIds = colorIds == null
                ? List.of()
                : colorIds.stream().filter(id -> id != null).distinct().toList();
        return normalize(new ProductSearchCondition(
                categoryId,
                keyword == null ? null : keyword.trim(),
                inStockOnly,
                bands,
                uniqueColorIds,
                categoryFilter == null ? ProductCategoryFilter.empty() : categoryFilter.normalize(),
                ProductSort.fromValue(sort, defaultSort),
                page,
                size,
                saleStartFrom,
                null
        ));
    }

    /**
     * 検索条件の必須値とページ情報を補完する。
     *
     * @param condition 検索条件
     * @return 正規化済み条件
     */
    private ProductSearchCondition normalize(ProductSearchCondition condition) {
        int page = Math.max(condition.page(), 1);
        int size = normalizeSize(condition.size());
        return new ProductSearchCondition(
                condition.categoryId(),
                condition.keyword(),
                condition.inStockOnly(),
                condition.priceBands() == null ? List.of() : condition.priceBands(),
                condition.colorIds() == null ? List.of() : condition.colorIds(),
                condition.categoryFilter() == null ? ProductCategoryFilter.empty() : condition.categoryFilter().normalize(),
                condition.sort() == null ? ProductSort.RECOMMENDED : condition.sort(),
                page,
                size,
                condition.saleStartFrom(),
                condition.tasteNames()
        );
    }

    /**
     * 新着商品の基準となる販売開始日時下限を返す。
     *
     * @return 現在日時の6か月前
     */
    private OffsetDateTime newArrivalSaleStartFrom() {
        return appTimeProvider.nowOffsetDateTime().minusMonths(6);
    }

    /**
     * 一覧画面で許可された表示件数だけを通す。
     *
     * @param requestedSize 要求表示件数
     * @return 正規化済み表示件数
     */
    private int normalizeSize(int requestedSize) {
        for (int size : ALLOWED_PAGE_SIZES) {
            if (size == requestedSize) {
                return size;
            }
        }
        return ALLOWED_PAGE_SIZES[0];
    }
}



