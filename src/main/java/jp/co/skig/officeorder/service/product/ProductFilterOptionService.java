package jp.co.skig.officeorder.service.product;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.springframework.stereotype.Service;

/**
 * 商品一覧の絞り込み候補を取得し、画面入力値を正規化するサービス。
 *
 * <p>カテゴリ固有条件やカラー条件の「選択可能な値」をここで定義し、
 * 画面から渡された生値を検索条件として安全に使える形へそろえる。
 */
@Service
public class ProductFilterOptionService {

    /** デスク幅の許容レンジID。 */
    private static final List<Integer> ALLOWED_DESK_WIDTH_BAND_IDS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
    /** デスク奥行の許容レンジID。 */
    private static final List<Integer> ALLOWED_DESK_DEPTH_BAND_IDS = List.of(1, 2, 3, 4, 5, 6);
    /** デスク高さの許容レンジID。 */
    private static final List<Integer> ALLOWED_DESK_HEIGHT_BAND_IDS = List.of(1, 2, 3, 4);

    /** 絞り込み候補マスタの取得窓口。 */
    private final ProductFilterOptionRepository productFilterOptionRepository;

    /**
     * 商品絞り込み候補サービスを生成する。
     *
     * @param productFilterOptionRepository 絞り込み候補マスタ取得リポジトリ
     */
    public ProductFilterOptionService(ProductFilterOptionRepository productFilterOptionRepository) {
        this.productFilterOptionRepository = productFilterOptionRepository;
    }

    /**
     * 画面描画に必要な絞り込み候補一式を読み込む。
     *
     * @return カラーとカテゴリ固有条件をまとめた候補群
     */
    public ProductFilterOptionsBundle loadOptionsBundle() {
        return new ProductFilterOptionsBundle(
                productFilterOptionRepository.findActiveColorOptions(),
                productFilterOptionRepository.findActiveDeskTopShapeOptions(),
                productFilterOptionRepository.findActiveChairFunctionOptions(),
                productFilterOptionRepository.findActiveChairMaterialOptions(),
                productFilterOptionRepository.findActiveStorageUsageOptions(),
                productFilterOptionRepository.findActiveTasteOptions()
        );
    }

    /**
     * デスク一覧向けのカテゴリ固有絞り込み条件を組み立てる。
     *
     * @param rawDeskTopShapeIds 天板形状の生入力値
     * @param rawDeskWidthBandIds 幅レンジの生入力値
     * @param rawDeskDepthBandIds 奥行レンジの生入力値
     * @param rawDeskHeightBandIds 高さレンジの生入力値
     * @param rawTasteIds テイストの生入力値
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildDeskFilter(List<Integer> rawDeskTopShapeIds,
                                                 List<Integer> rawDeskWidthBandIds,
                                                 List<Integer> rawDeskDepthBandIds,
                                                 List<Integer> rawDeskHeightBandIds,
                                                 List<Integer> rawTasteIds) {
        return buildDeskFilter(rawDeskTopShapeIds,
                rawDeskWidthBandIds,
                rawDeskDepthBandIds,
                rawDeskHeightBandIds,
                rawTasteIds,
                loadOptionsBundle());
    }

    /**
     * 候補群を指定してデスク一覧向け絞り込み条件を組み立てる。
     *
     * @param rawDeskTopShapeIds 天板形状の生入力値
     * @param rawDeskWidthBandIds 幅レンジの生入力値
     * @param rawDeskDepthBandIds 奥行レンジの生入力値
     * @param rawDeskHeightBandIds 高さレンジの生入力値
     * @param rawTasteIds テイストの生入力値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildDeskFilter(List<Integer> rawDeskTopShapeIds,
                                                 List<Integer> rawDeskWidthBandIds,
                                                 List<Integer> rawDeskDepthBandIds,
                                                 List<Integer> rawDeskHeightBandIds,
                                                 List<Integer> rawTasteIds,
                                                 ProductFilterOptionsBundle optionsBundle) {
        List<Integer> allowedDeskTopShapeIds = extractOptionIds(optionsBundle.deskTopShapeOptions());
        List<Integer> allowedTasteIds = extractOptionIds(optionsBundle.tasteOptions());
        return new ProductCategoryFilter(
                normalizeIntegerOptions(rawDeskTopShapeIds, allowedDeskTopShapeIds),
                normalizeIntegerOptions(rawDeskWidthBandIds, ALLOWED_DESK_WIDTH_BAND_IDS),
                normalizeIntegerOptions(rawDeskDepthBandIds, ALLOWED_DESK_DEPTH_BAND_IDS),
                normalizeIntegerOptions(rawDeskHeightBandIds, ALLOWED_DESK_HEIGHT_BAND_IDS),
                List.of(),
                List.of(),
                List.of(),
                toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))
        );
    }

    /**
     * チェア一覧向けのカテゴリ固有絞り込み条件を組み立てる。
     *
     * @param rawChairFunctionIds 機能の生入力値
     * @param rawChairMaterialIds 素材の生入力値
     * @param rawTasteIds テイストの生入力値
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildChairFilter(List<Integer> rawChairFunctionIds,
                                                  List<Integer> rawChairMaterialIds,
                                                  List<Integer> rawTasteIds) {
        return buildChairFilter(rawChairFunctionIds,
                rawChairMaterialIds,
                rawTasteIds,
                loadOptionsBundle());
    }

    /**
     * 候補群を指定してチェア一覧向け絞り込み条件を組み立てる。
     *
     * @param rawChairFunctionIds 機能の生入力値
     * @param rawChairMaterialIds 素材の生入力値
     * @param rawTasteIds テイストの生入力値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildChairFilter(List<Integer> rawChairFunctionIds,
                                                  List<Integer> rawChairMaterialIds,
                                                  List<Integer> rawTasteIds,
                                                  ProductFilterOptionsBundle optionsBundle) {
        List<Integer> allowedChairFunctionIds = extractOptionIds(optionsBundle.chairFunctionOptions());
        List<Integer> allowedChairMaterialIds = extractOptionIds(optionsBundle.chairMaterialOptions());
        List<Integer> allowedTasteIds = extractOptionIds(optionsBundle.tasteOptions());
        return new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                normalizeIntegerOptions(rawChairFunctionIds, allowedChairFunctionIds),
                normalizeIntegerOptions(rawChairMaterialIds, allowedChairMaterialIds),
                List.of(),
                toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))
        );
    }

    /**
     * 収納家具一覧向けのカテゴリ固有絞り込み条件を組み立てる。
     *
     * @param rawStorageUsageIds 用途の生入力値
     * @param rawTasteIds テイストの生入力値
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildStorageFilter(List<Integer> rawStorageUsageIds,
                                                    List<Integer> rawTasteIds) {
        return buildStorageFilter(rawStorageUsageIds, rawTasteIds, loadOptionsBundle());
    }

    /**
     * 候補群を指定して収納家具一覧向け絞り込み条件を組み立てる。
     *
     * @param rawStorageUsageIds 用途の生入力値
     * @param rawTasteIds テイストの生入力値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildStorageFilter(List<Integer> rawStorageUsageIds,
                                                    List<Integer> rawTasteIds,
                                                    ProductFilterOptionsBundle optionsBundle) {
        List<Integer> allowedStorageUsageIds = extractOptionIds(optionsBundle.storageUsageOptions());
        List<Integer> allowedTasteIds = extractOptionIds(optionsBundle.tasteOptions());
        return new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                normalizeIntegerOptions(rawStorageUsageIds, allowedStorageUsageIds),
                toLongList(normalizeIntegerOptions(rawTasteIds, allowedTasteIds))
        );
    }

    /**
     * 価格帯IDを有効なレンジだけに絞り込む。
     *
     * @param rawPriceBandIds 画面から渡された価格帯ID
     * @return 正規化済み価格帯ID
     */
    public List<Integer> normalizePriceBandIds(List<Integer> rawPriceBandIds) {
        if (rawPriceBandIds == null || rawPriceBandIds.isEmpty()) {
            return List.of();
        }
        return rawPriceBandIds.stream()
                .filter(id -> id != null)
                .filter(id -> PriceBand.fromId(id) != null)
                .distinct()
                .toList();
    }

    /**
     * カラーキーを現在有効なカラー候補に基づいて正規化する。
     *
     * @param rawColorKeys 画面から渡されたカラーキー
     * @return 正規化済みカラーキー
     */
    public List<String> normalizeColorKeys(List<String> rawColorKeys) {
        return normalizeColorKeys(rawColorKeys, loadOptionsBundle());
    }

    /**
     * カラーキーを指定候補群に基づいて正規化する。
     *
     * @param rawColorKeys 画面から渡されたカラーキー
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカラーキー
     */
    public List<String> normalizeColorKeys(List<String> rawColorKeys,
                                           ProductFilterOptionsBundle optionsBundle) {
        if (rawColorKeys == null || rawColorKeys.isEmpty()) {
            return List.of();
        }
        Set<String> allowedColorKeys = optionsBundle.colorOptions().stream()
                .map(ColorFilterOption::key)
                .filter(key -> key != null && !key.isBlank())
                .collect(Collectors.toSet());
        return rawColorKeys.stream()
                .filter(key -> key != null && allowedColorKeys.contains(key))
                .distinct()
                .toList();
    }

    /**
     * 画面のカラーキーを color_id に変換する。
     *
     * @param colorKeys カラーキー一覧
     * @return 正規化済み color_id 一覧
     */
    public List<Long> resolveColorIds(List<String> colorKeys) {
        return resolveColorIds(colorKeys, loadOptionsBundle());
    }

    /**
     * 画面のカラーキーを指定候補群で color_id に変換する。
     *
     * @param colorKeys カラーキー一覧
     * @param optionsBundle 使用する候補群
     * @return 正規化済み color_id 一覧
     */
    public List<Long> resolveColorIds(List<String> colorKeys,
                                      ProductFilterOptionsBundle optionsBundle) {
        if (colorKeys == null || colorKeys.isEmpty()) {
            return List.of();
        }
        return normalizeColorKeys(colorKeys, optionsBundle).stream()
                .map(this::parseLongOrNull)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    /**
     * 画面再表示用に、選択済み color_id をカラーキー順へ戻す。
     *
     * @param colorIds 選択済み color_id
     * @return 画面表示順に整えたカラーキー
     */
    public List<String> resolveSelectedColorKeys(List<Long> colorIds) {
        return resolveSelectedColorKeys(colorIds, loadOptionsBundle());
    }

    /**
     * 指定候補群を使って、選択済み color_id をカラーキー順へ戻す。
     *
     * @param colorIds 選択済み color_id
     * @param optionsBundle 使用する候補群
     * @return 画面表示順に整えたカラーキー
     */
    public List<String> resolveSelectedColorKeys(List<Long> colorIds,
                                                 ProductFilterOptionsBundle optionsBundle) {
        if (colorIds == null || colorIds.isEmpty()) {
            return List.of();
        }
        Set<Long> selectedColorIds = colorIds.stream()
                .filter(value -> value != null)
                .collect(Collectors.toSet());
        Map<String, Boolean> selectedKeys = new LinkedHashMap<>();
        for (ColorFilterOption option : optionsBundle.colorOptions()) {
            Long colorId = parseLongOrNull(option.key());
            if (colorId != null && selectedColorIds.contains(colorId)) {
                selectedKeys.put(option.key(), true);
            }
        }
        return List.copyOf(selectedKeys.keySet());
    }

    /**
     * カラー候補一覧を返す。
     *
     * @return カラー候補
     */
    public List<ColorFilterOption> colorFilterOptions() {
        return loadOptionsBundle().colorOptions();
    }

    /**
     * デスクの天板形状候補を返す。
     *
     * @return 天板形状候補
     */
    public List<CategoryFilterOption> deskTopShapeOptions() {
        return loadOptionsBundle().deskTopShapeOptions();
    }

    /**
     * チェアの機能候補を返す。
     *
     * @return 機能候補
     */
    public List<CategoryFilterOption> chairFunctionOptions() {
        return loadOptionsBundle().chairFunctionOptions();
    }

    /**
     * チェアの素材候補を返す。
     *
     * @return 素材候補
     */
    public List<CategoryFilterOption> chairMaterialOptions() {
        return loadOptionsBundle().chairMaterialOptions();
    }

    /**
     * デスクのテイスト候補を返す。（後方互換のため残す。tasteOptions() を使用すること）
     *
     * @return テイスト候補
     * @deprecated tasteOptions() を使用すること
     */
    @Deprecated
    public List<CategoryFilterOption> deskTasteOptions() {
        return loadOptionsBundle().tasteOptions();
    }

    /**
     * チェアのテイスト候補を返す。（後方互換のため残す。tasteOptions() を使用すること）
     *
     * @return テイスト候補
     * @deprecated tasteOptions() を使用すること
     */
    @Deprecated
    public List<CategoryFilterOption> chairTasteOptions() {
        return loadOptionsBundle().tasteOptions();
    }

    /**
     * 収納家具のテイスト候補を返す。（後方互換のため残す。tasteOptions() を使用すること）
     *
     * @return テイスト候補
     * @deprecated tasteOptions() を使用すること
     */
    @Deprecated
    public List<CategoryFilterOption> storageTasteOptions() {
        return loadOptionsBundle().tasteOptions();
    }

    /**
     * テイスト候補（統合）を返す。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> tasteOptions() {
        return loadOptionsBundle().tasteOptions();
    }

    /**
     * 検索結果画面（カテゴリ横断）のテイスト絞込ID一覧を正規化する。
     *
     * @param rawTasteIds 画面から渡された生値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みテイストID一覧（Long）
     */
    public List<Long> normalizeTasteIds(List<Integer> rawTasteIds,
                                        ProductFilterOptionsBundle optionsBundle) {
        if (rawTasteIds == null || rawTasteIds.isEmpty()) {
            return List.of();
        }
        List<Integer> allowedIds = extractOptionIds(optionsBundle.tasteOptions());
        return toLongList(normalizeIntegerOptions(rawTasteIds, allowedIds));
    }

    /**
     * 選択済み tasteIds を画面表示順のまま返す。
     *
     * @param tasteIds 選択済みID
     * @param optionsBundle 使用する候補群
     * @return 表示順に整えた選択済みID
     */
    public List<Long> resolveSelectedTasteIds(List<Long> tasteIds,
                                              ProductFilterOptionsBundle optionsBundle) {
        if (tasteIds == null || tasteIds.isEmpty()) {
            return List.of();
        }
        Set<Long> selected = new HashSet<>(tasteIds);
        return optionsBundle.tasteOptions().stream()
                .map(opt -> (long) opt.id())
                .filter(selected::contains)
                .toList();
    }

    /**
     * Integer リストを Long リストへ変換する。
     *
     * @param values 変換対象
     * @return Long リスト
     */
    private List<Long> toLongList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(Long::valueOf).toList();
    }

    /**
     * 許容値に含まれる整数選択肢だけを残す。
     *
     * @param rawValues 画面から渡された生値
     * @param allowedValues 許容する値
     * @return 正規化済み値一覧
     */
    private List<Integer> normalizeIntegerOptions(List<Integer> rawValues, List<Integer> allowedValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        return rawValues.stream()
                .filter(v -> v != null && allowedValues.contains(v))
                .distinct()
                .toList();
    }

    /**
     * 候補オブジェクト群からIDだけを取り出す。
     *
     * @param options 候補一覧
     * @return 候補ID一覧
     */
    private List<Integer> extractOptionIds(List<CategoryFilterOption> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        return options.stream()
                .map(CategoryFilterOption::id)
                .distinct()
                .toList();
    }

    /**
     * 数値文字列を {@link Long} に変換する。
     *
     * @param value 変換対象文字列
     * @return 変換結果。変換不可の場合は {@code null}
     */
    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
