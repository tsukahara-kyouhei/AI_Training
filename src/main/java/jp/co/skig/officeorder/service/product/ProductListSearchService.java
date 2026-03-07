package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductListSearchResult;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品一覧画面の検索条件組み立てとページ補正を行うサービス。
 *
 * <p>Controller が受け取った生のリクエスト値を、
 * 画面仕様に沿った {@link ProductSearchCondition} へ変換する役割を持つ。
 */
@Service
public class ProductListSearchService {

    /** 商品検索そのものを担当するサービス。 */
    private final ProductService productService;
    /** 画面の絞り込み入力値を正規化するサービス。 */
    private final ProductFilterOptionService productFilterOptionService;

    /**
     * 商品一覧検索サービスを生成する。
     *
     * @param productService 商品検索サービス
     * @param productFilterOptionService 絞り込み候補・正規化サービス
     */
    public ProductListSearchService(ProductService productService,
                                    ProductFilterOptionService productFilterOptionService) {
        this.productService = productService;
        this.productFilterOptionService = productFilterOptionService;
    }

    /**
     * 共通一覧向け検索条件を組み立てる。
     *
     * @param categoryId カテゴリID
     * @param keyword キーワード
     * @param inStockOnly 在庫ありのみ条件
     * @param rawPriceBandIds 価格帯の生入力値
     * @param rawColorKeys カラーの生入力値
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @param defaultSort デフォルト並び順
     * @param optionsBundle 使用する絞り込み候補群
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildCondition(String categoryId,
                                                 String keyword,
                                                 boolean inStockOnly,
                                                 List<Integer> rawPriceBandIds,
                                                 List<String> rawColorKeys,
                                                 String sort,
                                                 int page,
                                                 int size,
                                                 ProductSort defaultSort,
                                                 ProductFilterOptionsBundle optionsBundle) {
        return buildCondition(
                categoryId,
                keyword,
                inStockOnly,
                rawPriceBandIds,
                rawColorKeys,
                ProductCategoryFilter.empty(),
                sort,
                page,
                size,
                defaultSort,
                optionsBundle
        );
    }

    /**
     * 新着商品一覧向け検索条件を組み立てる。
     *
     * @param inStockOnly 在庫ありのみ条件
     * @param rawPriceBandIds 価格帯の生入力値
     * @param rawColorKeys カラーの生入力値
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @param optionsBundle 使用する絞り込み候補群
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildNewArrivalCondition(boolean inStockOnly,
                                                           List<Integer> rawPriceBandIds,
                                                           List<String> rawColorKeys,
                                                           String sort,
                                                           int page,
                                                           int size,
                                                           ProductFilterOptionsBundle optionsBundle) {
        List<Integer> selectedPriceBandIds = productFilterOptionService.normalizePriceBandIds(rawPriceBandIds);
        ProductFilterOptionsBundle resolvedBundle = optionsBundle == null
                ? productFilterOptionService.loadOptionsBundle()
                : optionsBundle;
        List<String> selectedColorKeys = productFilterOptionService.normalizeColorKeys(rawColorKeys, resolvedBundle);
        List<Long> colorIds = productFilterOptionService.resolveColorIds(selectedColorKeys, resolvedBundle);
        return productService.buildNewArrivalCondition(
                inStockOnly,
                selectedPriceBandIds,
                colorIds,
                sort,
                page,
                size
        );
    }

    /**
     * カテゴリ固有条件を含む検索条件を組み立てる。
     *
     * @param categoryId カテゴリID
     * @param keyword キーワード
     * @param inStockOnly 在庫ありのみ条件
     * @param rawPriceBandIds 価格帯の生入力値
     * @param rawColorKeys カラーの生入力値
     * @param categoryFilter カテゴリ固有条件
     * @param sort 並び順
     * @param page ページ番号
     * @param size 表示件数
     * @param defaultSort デフォルト並び順
     * @param optionsBundle 使用する絞り込み候補群
     * @return 正規化済み検索条件
     */
    public ProductSearchCondition buildCondition(String categoryId,
                                                 String keyword,
                                                 boolean inStockOnly,
                                                 List<Integer> rawPriceBandIds,
                                                 List<String> rawColorKeys,
                                                 ProductCategoryFilter categoryFilter,
                                                 String sort,
                                                 int page,
                                                 int size,
                                                 ProductSort defaultSort,
                                                 ProductFilterOptionsBundle optionsBundle) {
        List<Integer> selectedPriceBandIds = productFilterOptionService.normalizePriceBandIds(rawPriceBandIds);
        ProductFilterOptionsBundle resolvedBundle = optionsBundle == null
                ? productFilterOptionService.loadOptionsBundle()
                : optionsBundle;
        List<String> selectedColorKeys = productFilterOptionService.normalizeColorKeys(rawColorKeys, resolvedBundle);
        List<Long> colorIds = productFilterOptionService.resolveColorIds(selectedColorKeys, resolvedBundle);
        return productService.buildCondition(
                categoryId,
                keyword,
                inStockOnly,
                selectedPriceBandIds,
                colorIds,
                sort,
                page,
                size,
                defaultSort,
                categoryFilter
        );
    }

    /**
     * 検索結果を取得し、ページ超過時は最終ページへ補正して再検索する。
     *
     * <p>絞り込み変更や件数変動で存在しないページ番号が指定されても、
     * 空一覧のままにせず最後に存在するページを表示する。
     *
     * @param condition 検索条件
     * @return 補正後条件を含む検索結果
     */
    public ProductListSearchResult searchWithPageCorrection(ProductSearchCondition condition) {
        ProductListPage first = productService.search(condition);
        int totalPages = calculateTotalPages(first.totalCount(), condition.size());
        if (first.totalCount() > 0 && first.items().isEmpty() && condition.page() > totalPages) {
            ProductSearchCondition corrected = new ProductSearchCondition(
                    condition.categoryId(),
                    condition.keyword(),
                    condition.inStockOnly(),
                    condition.priceBands(),
                    condition.colorIds(),
                    condition.categoryFilter(),
                    condition.sort(),
                    totalPages,
                    condition.size(),
                    condition.saleStartFrom()
            );
            return new ProductListSearchResult(corrected, productService.search(corrected));
        }
        return new ProductListSearchResult(condition, first);
    }

    /**
     * 総件数と表示件数から総ページ数を計算する。
     *
     * @param totalCount 総件数
     * @param size 1ページ件数
     * @return 総ページ数。0件でも 1 を返す
     */
    public int calculateTotalPages(long totalCount, int size) {
        if (totalCount <= 0) {
            return 1;
        }
        return (int) ((totalCount + size - 1) / size);
    }
}
