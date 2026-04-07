package jp.co.skig.officeorder.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.mapper.row.ProductColorCodeMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductDetailMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductFilterColorOptionMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductFilterOptionMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductListMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductRankedMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductSeriesLinkMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductVariantMapperRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品一覧、商品詳細、ランキング、絞り込み条件取得に関する SQL を集約した Mapper。
 */
@Mapper
public interface ProductMapper {

    /**
     * 検索条件に一致する商品の総件数を取得する。
     */
    Long countProducts(Map<String, Object> params);

    /**
     * 一覧表示用に、検索条件に一致する商品行を取得する。
     */
    List<ProductListMapperRow> selectProducts(Map<String, Object> params);

    /**
     * 商品IDを指定して、トップ表示や関連商品表示に必要な商品行を取得する。
     */
    List<ProductListMapperRow> selectProductsByIds(@Param("productIds") List<Long> productIds,
                                                   @Param("now") OffsetDateTime now);

    /**
     * 一覧カードに表示するカラーコードを商品単位で取得する。
     */
    List<ProductColorCodeMapperRow> selectColorCodes(@Param("productIds") List<Long> productIds);

    /**
     * 保存済みの売れ筋ランキング上位商品を取得する。
     */
    List<ProductRankedMapperRow> selectTopRankedProducts(@Param("limit") int limit);

    /**
     * 商品詳細画面のヘッダ部に必要な商品基本情報を取得する。
     */
    ProductDetailMapperRow selectProductDetail(@Param("productId") long productId,
                                               @Param("now") OffsetDateTime now);

    /**
     * 商品詳細で選択可能なカラー別バリアントを取得する。
     */
    List<ProductVariantMapperRow> selectProductVariants(@Param("productId") long productId);

    /**
     * 同一バリエーショングループの商品リンクを取得する。
     */
    List<ProductSeriesLinkMapperRow> selectSeriesLinks(@Param("variationGroupId") long variationGroupId,
                                                       @Param("now") OffsetDateTime now);

    /**
     * 保存済みのおすすめ関連商品を取得する。
     */
    List<ProductRankedMapperRow> selectRecommendedProducts(@Param("sourceProductId") long sourceProductId,
                                                           @Param("limit") int limit);

    /**
     * 一覧絞り込みで使用するカラー選択肢を取得する。
     */
    List<ProductFilterColorOptionMapperRow> selectActiveColorFilterOptions();

    /**
     * デスク一覧の天板形状選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveDeskTopShapeOptions();

    /**
     * デスク一覧のテイスト選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveDeskTasteOptions();

    /**
     * チェア一覧の機能選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveChairFunctionOptions();

    /**
     * チェア一覧の素材選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveChairMaterialOptions();

    /**
     * チェア一覧のテイスト選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveChairTasteOptions();

    /**
     * 収納家具一覧の用途選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveStorageUsageOptions();

    /**
     * 収納家具一覧のテイスト選択肢を取得する。
     */
    List<ProductFilterOptionMapperRow> selectActiveStorageTasteOptions();

    /**
     * 検索結果画面用のテイスト統合選択肢を取得する。
     *
     * <p>desk_tastes / chair_tastes / storage_tastes を display_name で UNION し、
     * 重複排除・ソート済みのテイスト名称一覧を返す。
     */
    List<String> selectActiveSearchTasteOptions();

    /**
     * 指定時点で有効な消費税率を取得する。
     */
    BigDecimal selectCurrentTaxRatePercent(@Param("now") OffsetDateTime now);
}


