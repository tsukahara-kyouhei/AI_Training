package jp.co.skig.officeorder.service.product;

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
                productFilterOptionRepository.findActiveDeskTasteOptions(),
                productFilterOptionRepository.findActiveChairFunctionOptions(),
                productFilterOptionRepository.findActiveChairMaterialOptions(),
                productFilterOptionRepository.findActiveChairTasteOptions(),
                productFilterOptionRepository.findActiveStorageUsageOptions(),
                productFilterOptionRepository.findActiveStorageTasteOptions()
        );
    }

    /**
     * デスク一覧向けのカテゴリ固有絞り込み条件を組み立てる。
     *
     * @param rawDeskTopShapeIds 天板形状の生入力値
     * @param rawDeskWidthBandIds 幅レンジの生入力値
     * @param rawDeskDepthBandIds 奥行レンジの生入力値
     * @param rawDeskHeightBandIds 高さレンジの生入力値
     * @param rawDeskTasteIds テイストの生入力値
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildDeskFilter(List<Integer> rawDeskTopShapeIds,
                                                 List<Integer> rawDeskWidthBandIds,
                                                 List<Integer> rawDeskDepthBandIds,
                                                 List<Integer> rawDeskHeightBandIds,
                                                 List<Integer> rawDeskTasteIds) {
        return buildDeskFilter(rawDeskTopShapeIds,
                rawDeskWidthBandIds,
                rawDeskDepthBandIds,
                rawDeskHeightBandIds,
                rawDeskTasteIds,
                loadOptionsBundle());
    }

    /**
     * 候補群を指定してデスク一覧向け絞り込み条件を組み立てる。
     *
     * @param rawDeskTopShapeIds 天板形状の生入力値
     * @param rawDeskWidthBandIds 幅レンジの生入力値
     * @param rawDeskDepthBandIds 奥行レンジの生入力値
     * @param rawDeskHeightBandIds 高さレンジの生入力値
     * @param rawDeskTasteIds テイストの生入力値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildDeskFilter(List<Integer> rawDeskTopShapeIds,
                                                 List<Integer> rawDeskWidthBandIds,
                                                 List<Integer> rawDeskDepthBandIds,
                                                 List<Integer> rawDeskHeightBandIds,
                                                 List<Integer> rawDeskTasteIds,
                                                 ProductFilterOptionsBundle optionsBundle) {
        List<Integer> allowedDeskTopShapeIds = extractOptionIds(optionsBundle.deskTopShapeOptions());
        List<Integer> allowedDeskTasteIds = extractOptionIds(optionsBundle.deskTasteOptions());
        return new ProductCategoryFilter(
                normalizeIntegerOptions(rawDeskTopShapeIds, allowedDeskTopShapeIds),
                normalizeIntegerOptions(rawDeskWidthBandIds, ALLOWED_DESK_WIDTH_BAND_IDS),
                normalizeIntegerOptions(rawDeskDepthBandIds, ALLOWED_DESK_DEPTH_BAND_IDS),
                normalizeIntegerOptions(rawDeskHeightBandIds, ALLOWED_DESK_HEIGHT_BAND_IDS),
                normalizeIntegerOptions(rawDeskTasteIds, allowedDeskTasteIds),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    /**
     * チェア一覧向けのカテゴリ固有絞り込み条件を組み立てる。
     *
     * @param rawChairFunctionIds 機能の生入力値
     * @param rawChairMaterialIds 素材の生入力値
     * @param rawChairTasteIds テイストの生入力値
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildChairFilter(List<Integer> rawChairFunctionIds,
                                                  List<Integer> rawChairMaterialIds,
                                                  List<Integer> rawChairTasteIds) {
        return buildChairFilter(rawChairFunctionIds,
                rawChairMaterialIds,
                rawChairTasteIds,
                loadOptionsBundle());
    }

    /**
     * 候補群を指定してチェア一覧向け絞り込み条件を組み立てる。
     *
     * @param rawChairFunctionIds 機能の生入力値
     * @param rawChairMaterialIds 素材の生入力値
     * @param rawChairTasteIds テイストの生入力値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildChairFilter(List<Integer> rawChairFunctionIds,
                                                  List<Integer> rawChairMaterialIds,
                                                  List<Integer> rawChairTasteIds,
                                                  ProductFilterOptionsBundle optionsBundle) {
        List<Integer> allowedChairFunctionIds = extractOptionIds(optionsBundle.chairFunctionOptions());
        List<Integer> allowedChairMaterialIds = extractOptionIds(optionsBundle.chairMaterialOptions());
        List<Integer> allowedChairTasteIds = extractOptionIds(optionsBundle.chairTasteOptions());
        return new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                normalizeIntegerOptions(rawChairFunctionIds, allowedChairFunctionIds),
                normalizeIntegerOptions(rawChairMaterialIds, allowedChairMaterialIds),
                normalizeIntegerOptions(rawChairTasteIds, allowedChairTasteIds),
                List.of(),
                List.of()
        );
    }

    /**
     * 収納家具一覧向けのカテゴリ固有絞り込み条件を組み立てる。
     *
     * @param rawStorageUsageIds 用途の生入力値
     * @param rawStorageTasteIds テイストの生入力値
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildStorageFilter(List<Integer> rawStorageUsageIds,
                                                    List<Integer> rawStorageTasteIds) {
        return buildStorageFilter(rawStorageUsageIds, rawStorageTasteIds, loadOptionsBundle());
    }

    /**
     * 候補群を指定して収納家具一覧向け絞り込み条件を組み立てる。
     *
     * @param rawStorageUsageIds 用途の生入力値
     * @param rawStorageTasteIds テイストの生入力値
     * @param optionsBundle 使用する候補群
     * @return 正規化済みカテゴリ条件
     */
    public ProductCategoryFilter buildStorageFilter(List<Integer> rawStorageUsageIds,
                                                    List<Integer> rawStorageTasteIds,
                                                    ProductFilterOptionsBundle optionsBundle) {
        List<Integer> allowedStorageUsageIds = extractOptionIds(optionsBundle.storageUsageOptions());
        List<Integer> allowedStorageTasteIds = extractOptionIds(optionsBundle.storageTasteOptions());
        return new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                normalizeIntegerOptions(rawStorageUsageIds, allowedStorageUsageIds),
                normalizeIntegerOptions(rawStorageTasteIds, allowedStorageTasteIds)
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
     * 検索結果画面向けにカテゴリ横断のテイスト候補一覧を返す。
     *
     * @param optionsBundle 使用する候補群
     * @return 重複排除済みテイスト名一覧
     */
    public List<String> buildSearchTasteOptions(ProductFilterOptionsBundle optionsBundle) {
        ProductFilterOptionsBundle resolvedBundle = optionsBundle == null
                ? loadOptionsBundle()
                : optionsBundle;
        Map<String, Boolean> mergedLabels = new LinkedHashMap<>();
        collectTasteLabels(mergedLabels, resolvedBundle.deskTasteOptions());
        collectTasteLabels(mergedLabels, resolvedBundle.chairTasteOptions());
        collectTasteLabels(mergedLabels, resolvedBundle.storageTasteOptions());
        return List.copyOf(mergedLabels.keySet());
    }

    /**
     * 検索結果画面の生テイスト名を候補に沿って正規化する。
     *
     * @param rawTasteNames 生テイスト名一覧
     * @param optionsBundle 使用する候補群
     * @return 正規化済みテイスト名一覧
     */
    public List<String> normalizeSearchTasteNames(List<String> rawTasteNames,
                                                  ProductFilterOptionsBundle optionsBundle) {
        if (rawTasteNames == null || rawTasteNames.isEmpty()) {
            return List.of();
        }
        Set<String> allowedTasteNames = buildSearchTasteOptions(optionsBundle).stream()
                .collect(Collectors.toSet());
        return rawTasteNames.stream()
                .filter(value -> value != null)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .filter(allowedTasteNames::contains)
                .distinct()
                .toList();
    }

    /**
     * 検索結果画面向けのテイスト条件をカテゴリ横断で構築する。
     *
     * @param rawTasteNames 生テイスト名一覧
     * @param optionsBundle 使用する候補群
     * @return テイスト条件のみを保持したカテゴリ条件
     */
    public ProductCategoryFilter buildSearchTasteFilter(List<String> rawTasteNames,
                                                        ProductFilterOptionsBundle optionsBundle) {
        ProductFilterOptionsBundle resolvedBundle = optionsBundle == null
                ? loadOptionsBundle()
                : optionsBundle;
        List<String> selectedTasteNames = normalizeSearchTasteNames(rawTasteNames, resolvedBundle);
        if (selectedTasteNames.isEmpty()) {
            return ProductCategoryFilter.empty();
        }
        return new ProductCategoryFilter(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                resolveTasteIds(selectedTasteNames, resolvedBundle.deskTasteOptions()),
                List.of(),
                List.of(),
                resolveTasteIds(selectedTasteNames, resolvedBundle.chairTasteOptions()),
                List.of(),
                resolveTasteIds(selectedTasteNames, resolvedBundle.storageTasteOptions())
        );
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
     * デスクのテイスト候補を返す。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> deskTasteOptions() {
        return loadOptionsBundle().deskTasteOptions();
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
     * チェアのテイスト候補を返す。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> chairTasteOptions() {
        return loadOptionsBundle().chairTasteOptions();
    }

    /**
     * 収納家具の用途候補を返す。
     *
     * @return 用途候補
     */
    public List<CategoryFilterOption> storageUsageOptions() {
        return loadOptionsBundle().storageUsageOptions();
    }

    /**
     * 収納家具のテイスト候補を返す。
     *
     * @return テイスト候補
     */
    public List<CategoryFilterOption> storageTasteOptions() {
        return loadOptionsBundle().storageTasteOptions();
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

    private void collectTasteLabels(Map<String, Boolean> labels, List<CategoryFilterOption> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        for (CategoryFilterOption option : options) {
            if (option == null || option.label() == null) {
                continue;
            }
            String label = option.label().trim();
            if (!label.isEmpty()) {
                labels.putIfAbsent(label, true);
            }
        }
    }

    private List<Integer> resolveTasteIds(List<String> selectedTasteNames,
                                          List<CategoryFilterOption> options) {
        if (selectedTasteNames == null || selectedTasteNames.isEmpty() || options == null || options.isEmpty()) {
            return List.of();
        }
        Set<String> selected = selectedTasteNames.stream().collect(Collectors.toSet());
        return options.stream()
                .filter(option -> option != null && option.label() != null)
                .filter(option -> selected.contains(option.label().trim()))
                .map(CategoryFilterOption::id)
                .filter(id -> id != null)
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
