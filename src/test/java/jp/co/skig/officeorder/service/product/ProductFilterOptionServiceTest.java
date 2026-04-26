package jp.co.skig.officeorder.service.product;

import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link ProductFilterOptionService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    ProductFilterOptionRepository productFilterOptionRepository;

    ProductFilterOptionService sut;

    @BeforeEach
    void setUp() {
        sut = new ProductFilterOptionService(productFilterOptionRepository);
    }

    // ── normalizePriceBandIds ─────────────────────────────────────────────

    @Test
    void nullを渡すと空リストが返ること() {
        assertThat(sut.normalizePriceBandIds(null)).isEmpty();
    }

    @Test
    void 空リストを渡すと空リストが返ること() {
        assertThat(sut.normalizePriceBandIds(List.of())).isEmpty();
    }

    @Test
    void 有効な価格帯IDはそのまま返ること() {
        assertThat(sut.normalizePriceBandIds(List.of(1, 2, 3))).containsExactly(1, 2, 3);
    }

    @Test
    void 無効な価格帯IDは除外されること() {
        // PriceBand は1〜6 なので 99 は無効
        assertThat(sut.normalizePriceBandIds(List.of(1, 99, 3))).containsExactly(1, 3);
    }

    @Test
    void 重複した価格帯IDは1件に統一されること() {
        assertThat(sut.normalizePriceBandIds(List.of(1, 1, 2))).containsExactly(1, 2);
    }

    // ── normalizeColorKeys (with bundle) ──────────────────────────────────

    @Test
    void nullカラーキーを渡すと空リストが返ること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("red", "blue"));

        assertThat(sut.normalizeColorKeys(null, bundle)).isEmpty();
    }

    @Test
    void 空カラーキーリストを渡すと空リストが返ること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("red", "blue"));

        assertThat(sut.normalizeColorKeys(List.of(), bundle)).isEmpty();
    }

    @Test
    void 有効なカラーキーはそのまま返ること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("1", "2"));

        assertThat(sut.normalizeColorKeys(List.of("1"), bundle)).containsExactly("1");
    }

    @Test
    void 候補にないカラーキーは除外されること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("1", "2"));

        assertThat(sut.normalizeColorKeys(List.of("1", "999"), bundle)).containsExactly("1");
    }

    @Test
    void 重複したカラーキーは1件に統一されること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("1", "2"));

        assertThat(sut.normalizeColorKeys(List.of("1", "1", "2"), bundle)).containsExactly("1", "2");
    }

    // ── resolveColorIds ───────────────────────────────────────────────────

    @Test
    void nullカラーキーを渡すとcolorIdsが空になること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("1", "2"));

        assertThat(sut.resolveColorIds(null, bundle)).isEmpty();
    }

    @Test
    void 空カラーキーリストを渡すとcolorIdsが空になること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("1", "2"));

        assertThat(sut.resolveColorIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void 数値キーがLongに変換されること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("10", "20"));

        List<Long> ids = sut.resolveColorIds(List.of("10", "20"), bundle);

        assertThat(ids).containsExactly(10L, 20L);
    }

    @Test
    void 数値に変換できないキーは除外されること() {
        // カラーキー "abc" はlong変換不可
        ProductFilterOptionsBundle bundle = buildBundle(List.of("abc", "10"));

        List<Long> ids = sut.resolveColorIds(List.of("abc", "10"), bundle);

        assertThat(ids).containsExactly(10L);
    }

    // ── resolveSelectedColorKeys ──────────────────────────────────────────

    @Test
    void nullカラーIDを渡すとキー一覧が空になること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("1", "2"));

        assertThat(sut.resolveSelectedColorKeys(null, bundle)).isEmpty();
    }

    @Test
    void カラーIDからキーが逆引きできること() {
        // bundleのkey "10" は long 10 に対応
        ProductFilterOptionsBundle bundle = buildBundle(List.of("10", "20"));

        List<String> keys = sut.resolveSelectedColorKeys(List.of(10L), bundle);

        assertThat(keys).containsExactly("10");
    }

    @Test
    void 候補にないカラーIDは除外されること() {
        ProductFilterOptionsBundle bundle = buildBundle(List.of("10", "20"));

        List<String> keys = sut.resolveSelectedColorKeys(List.of(99L), bundle);

        assertThat(keys).isEmpty();
    }

    // ── buildDeskFilter ───────────────────────────────────────────────────

    @Test
    void 許可されたデスク幅レンジIDのみ通ること() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions();

        // 許可は 1..11 なので 99 は除外
        ProductCategoryFilter filter = sut.buildDeskFilter(
                null, List.of(1, 99), null, null, null, bundle);

        assertThat(filter.deskWidthBandIds()).containsExactly(1);
    }

    @Test
    void 許可されたデスク奥行レンジIDのみ通ること() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions();

        // 許可は 1..6 なので 7 は除外
        ProductCategoryFilter filter = sut.buildDeskFilter(
                null, null, List.of(1, 7), null, null, bundle);

        assertThat(filter.deskDepthBandIds()).containsExactly(1);
    }

    @Test
    void 許可されたデスク高さレンジIDのみ通ること() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions();

        // 許可は 1..4 なので 5 は除外
        ProductCategoryFilter filter = sut.buildDeskFilter(
                null, null, null, List.of(1, 5), null, bundle);

        assertThat(filter.deskHeightBandIds()).containsExactly(1);
    }

    @Test
    void デスクフィルタ以外のカテゴリ条件は空になること() {
        ProductFilterOptionsBundle bundle = buildBundleWithDeskOptions();

        ProductCategoryFilter filter = sut.buildDeskFilter(
                List.of(1), List.of(1), List.of(1), List.of(1), null, bundle);

        assertThat(filter.chairFunctionIds()).isEmpty();
        assertThat(filter.chairMaterialIds()).isEmpty();
        assertThat(filter.storageUsageIds()).isEmpty();
    }

    // ── buildChairFilter ──────────────────────────────────────────────────

    @Test
    void チェアフィルタ以外のカテゴリ条件は空になること() {
        ProductFilterOptionsBundle bundle = buildBundleWithChairOptions();

        ProductCategoryFilter filter = sut.buildChairFilter(
                List.of(1), List.of(1), List.of(1), bundle);

        assertThat(filter.deskWidthBandIds()).isEmpty();
        assertThat(filter.storageUsageIds()).isEmpty();
    }

    // ── buildStorageFilter ────────────────────────────────────────────────

    @Test
    void 収納フィルタ以外のカテゴリ条件は空になること() {
        ProductFilterOptionsBundle bundle = buildBundleWithStorageOptions();

        ProductCategoryFilter filter = sut.buildStorageFilter(
                List.of(1), List.of(1), bundle);

        assertThat(filter.deskWidthBandIds()).isEmpty();
        assertThat(filter.chairFunctionIds()).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private ProductFilterOptionsBundle buildBundle(List<String> colorKeys) {
        List<ColorFilterOption> colors = colorKeys.stream()
                .map(k -> new ColorFilterOption(k, k, "#000"))
                .toList();
        return new ProductFilterOptionsBundle(
                colors, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private ProductFilterOptionsBundle buildBundleWithDeskOptions() {
        List<ColorFilterOption> colors = List.of();
        List<CategoryFilterOption> deskTopShapeOptions = List.of(new CategoryFilterOption(1, "長方形"));
        List<CategoryFilterOption> deskTasteOptions = List.of(new CategoryFilterOption(1, "モダン"));
        return new ProductFilterOptionsBundle(
                colors, deskTopShapeOptions, deskTasteOptions,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private ProductFilterOptionsBundle buildBundleWithChairOptions() {
        List<CategoryFilterOption> chairFunctionOptions = List.of(new CategoryFilterOption(1, "リクライニング"));
        List<CategoryFilterOption> chairMaterialOptions = List.of(new CategoryFilterOption(1, "メッシュ"));
        List<CategoryFilterOption> chairTasteOptions = List.of(new CategoryFilterOption(1, "モダン"));
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(),
                chairFunctionOptions, chairMaterialOptions, chairTasteOptions,
                List.of(), List.of(), List.of()
        );
    }

    private ProductFilterOptionsBundle buildBundleWithStorageOptions() {
        List<CategoryFilterOption> storageUsageOptions = List.of(new CategoryFilterOption(1, "書類収納"));
        List<CategoryFilterOption> storageTasteOptions = List.of(new CategoryFilterOption(1, "シンプル"));
        return new ProductFilterOptionsBundle(
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                storageUsageOptions, storageTasteOptions, List.of()
        );
    }
}
