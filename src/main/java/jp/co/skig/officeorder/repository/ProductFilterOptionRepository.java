package jp.co.skig.officeorder.repository;

import java.util.List;

import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.mapper.row.ProductFilterColorOptionMapperRow;
import jp.co.skig.officeorder.mapper.row.ProductFilterOptionMapperRow;
import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import org.springframework.stereotype.Repository;

/**
 * 商品一覧絞り込み候補の取得と表示用整形を担当するリポジトリ。
 */
@Repository
public class ProductFilterOptionRepository {

    /** 透明色スウォッチ判定用タイプ名。 */
    private static final String SWATCH_TYPE_TRANSPARENT_PATTERN = "transparent_pattern";
    /** 透明色スウォッチ表示用CSSパターン。 */
    private static final String TRANSPARENT_COLOR_PATTERN =
            "repeating-linear-gradient(45deg, #d9d9d9, #d9d9d9 3px, #ffffff 3px, #ffffff 6px)";

    /** 商品絞り込み候補SQLを呼び出す Mapper。 */
    private final ProductMapper productMapper;

    /**
     * 絞り込み候補リポジトリを生成する。
     *
     * @param productMapper 商品Mapper
     */
    public ProductFilterOptionRepository(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 有効なカラー候補一覧を取得する。
     *
     * @return カラー候補
     */
    public List<ColorFilterOption> findActiveColorOptions() {
        return productMapper.selectActiveColorFilterOptions().stream()
                .map(this::toColorFilterOption)
                .toList();
    }

    /**
     * 有効なデスク天板形状候補を取得する。
     *
     * @return 天板形状候補
     */
    public List<CategoryFilterOption> findActiveDeskTopShapeOptions() {
        return productMapper.selectActiveDeskTopShapeOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * 有効なデスクテイスト候補を取得する。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> findActiveDeskTasteOptions() {
        return productMapper.selectActiveDeskTasteOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * 有効なチェア機能候補を取得する。
     *
     * @return 機能候補
     */
    public List<CategoryFilterOption> findActiveChairFunctionOptions() {
        return productMapper.selectActiveChairFunctionOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * 有効なチェア素材候補を取得する。
     *
     * @return 素材候補
     */
    public List<CategoryFilterOption> findActiveChairMaterialOptions() {
        return productMapper.selectActiveChairMaterialOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * 有効なチェアテイスト候補を取得する。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> findActiveChairTasteOptions() {
        return productMapper.selectActiveChairTasteOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * 有効な収納家具用途候補を取得する。
     *
     * @return 用途候補
     */
    public List<CategoryFilterOption> findActiveStorageUsageOptions() {
        return productMapper.selectActiveStorageUsageOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * 有効な収納家具テイスト候補を取得する。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> findActiveStorageTasteOptions() {
        return productMapper.selectActiveStorageTasteOptions().stream()
                .map(this::toCategoryFilterOption)
                .filter(option -> option != null)
                .toList();
    }

    /**
     * カテゴリ横断で統合されたテイスト表示名一覧を取得する。
     *
     * @return 重複排除・ソート済みのテイスト表示名一覧
     */
    public List<String> findUnifiedTasteDisplayNames() {
        return productMapper.selectUnifiedTasteDisplayNames();
    }

    /**
     * カラー候補行を表示用モデルへ変換する。
     *
     * @param row カラー候補行
     * @return 表示用カラー候補
     */
    private ColorFilterOption toColorFilterOption(ProductFilterColorOptionMapperRow row) {
        Long colorId = row.colorId();
        String colorName = row.colorName();
        String colorCode = row.colorCode();
        String swatchType = row.swatchType();
        return new ColorFilterOption(
                colorId == null ? null : String.valueOf(colorId),
                colorName,
                resolveColorSwatch(swatchType, colorCode)
        );
    }

    /**
     * カテゴリ候補行を表示用モデルへ変換する。
     *
     * @param row 候補行
     * @return 表示用候補
     */
    private CategoryFilterOption toCategoryFilterOption(ProductFilterOptionMapperRow row) {
        Integer id = row.optionId();
        if (id == null) {
            return null;
        }
        String label = row.displayName();
        return new CategoryFilterOption(id, label == null ? "" : label);
    }

    /**
     * スウォッチ種別に応じて画面表示用の色表現を返す。
     *
     * @param swatchType スウォッチ種別
     * @param colorCode 通常色コード
     * @return 表示用色表現
     */
    private String resolveColorSwatch(String swatchType, String colorCode) {
        if (SWATCH_TYPE_TRANSPARENT_PATTERN.equals(swatchType)) {
            return TRANSPARENT_COLOR_PATTERN;
        }
        return colorCode;
    }
}

