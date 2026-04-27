package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link ProductFilterOptionService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    private ProductFilterOptionRepository productFilterOptionRepository;

    @InjectMocks
    private ProductFilterOptionService service;

    private ProductFilterOptionsBundle bundle;

    @BeforeEach
    void setUp() {
        bundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("black", "ブラック", "#000000")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new CategoryFilterOption(1, "ベーシック"),
                        new CategoryFilterOption(2, "カジュアル"),
                        new CategoryFilterOption(3, "モダン")
                )
        );
    }

    // ---- normalizeTasteIds ----

    @Test
    void normalizeTasteIds_nullはemptyを返す() {
        assertThat(service.normalizeTasteIds(null, bundle)).isEmpty();
    }

    @Test
    void normalizeTasteIds_空リストはemptyを返す() {
        assertThat(service.normalizeTasteIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void normalizeTasteIds_有効なIDはLongリストに変換される() {
        List<Long> result = service.normalizeTasteIds(List.of(1, 2), bundle);
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void normalizeTasteIds_無効なIDは除外される() {
        List<Long> result = service.normalizeTasteIds(List.of(99), bundle);
        assertThat(result).isEmpty();
    }

    // ---- resolveSelectedTasteIds ----

    @Test
    void resolveSelectedTasteIds_nullはemptyを返す() {
        assertThat(service.resolveSelectedTasteIds(null, bundle)).isEmpty();
    }

    @Test
    void resolveSelectedTasteIds_空リストはemptyを返す() {
        assertThat(service.resolveSelectedTasteIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void resolveSelectedTasteIds_選択済みIDはオプションの順序で返す() {
        // 逆順で渡しても、オプション定義順 (1→2) で返される
        List<Long> result = service.resolveSelectedTasteIds(List.of(2L, 1L), bundle);
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void resolveSelectedTasteIds_オプションに存在しないIDは除外される() {
        List<Long> result = service.resolveSelectedTasteIds(List.of(1L, 99L), bundle);
        assertThat(result).containsExactly(1L);
    }

    // ---- normalizePriceBandIds ----

    @Test
    void normalizePriceBandIds_nullはemptyを返す() {
        assertThat(service.normalizePriceBandIds(null)).isEmpty();
    }

    @Test
    void normalizePriceBandIds_空リストはemptyを返す() {
        assertThat(service.normalizePriceBandIds(List.of())).isEmpty();
    }

    @Test
    void normalizePriceBandIds_有効なIDは通過する() {
        assertThat(service.normalizePriceBandIds(List.of(1))).containsExactly(1);
    }

    @Test
    void normalizePriceBandIds_無効なIDは除外される() {
        assertThat(service.normalizePriceBandIds(List.of(99))).isEmpty();
    }

    @Test
    void normalizePriceBandIds_重複は除去される() {
        assertThat(service.normalizePriceBandIds(List.of(1, 1, 2))).containsExactly(1, 2);
    }

    @Test
    void normalizePriceBandIds_すべての有効な価格帯IDが通過する() {
        assertThat(service.normalizePriceBandIds(List.of(1, 2, 3, 4, 5, 6)))
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    // ---- normalizeColorKeys ----

    @Test
    void normalizeColorKeys_nullはemptyを返す() {
        assertThat(service.normalizeColorKeys(null, bundle)).isEmpty();
    }

    @Test
    void normalizeColorKeys_空リストはemptyを返す() {
        assertThat(service.normalizeColorKeys(List.of(), bundle)).isEmpty();
    }

    @Test
    void normalizeColorKeys_有効なキーは通過する() {
        assertThat(service.normalizeColorKeys(List.of("black"), bundle)).containsExactly("black");
    }

    @Test
    void normalizeColorKeys_bundleに存在しないキーは除外される() {
        assertThat(service.normalizeColorKeys(List.of("white"), bundle)).isEmpty();
    }

    @Test
    void normalizeColorKeys_重複は除去される() {
        assertThat(service.normalizeColorKeys(List.of("black", "black"), bundle)).containsExactly("black");
    }

    @Test
    void normalizeColorKeys_大文字小文字は区別される() {
        // setupのbundleのキーは小文字"black"のみ
        assertThat(service.normalizeColorKeys(List.of("BLACK"), bundle)).isEmpty();
    }

    // ---- resolveColorIds ----

    @Test
    void resolveColorIds_nullはemptyを返す() {
        assertThat(service.resolveColorIds(null, bundle)).isEmpty();
    }

    @Test
    void resolveColorIds_空リストはemptyを返す() {
        assertThat(service.resolveColorIds(List.of(), bundle)).isEmpty();
    }

    @Test
    void resolveColorIds_非数値キーは変換できないため除外される() {
        // bundle のカラーキーは"black"（非数値）→ parseLongOrNull で null → 除外
        assertThat(service.resolveColorIds(List.of("black"), bundle)).isEmpty();
    }

    @Test
    void resolveColorIds_数値キーはLongに変換される() {
        // キーが数値文字列のbundleを使用
        ProductFilterOptionsBundle numericBundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("1", "ブラック", "#000000")),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        assertThat(service.resolveColorIds(List.of("1"), numericBundle)).containsExactly(1L);
    }

    // ---- resolveSelectedColorKeys ----

    @Test
    void resolveSelectedColorKeys_nullはemptyを返す() {
        assertThat(service.resolveSelectedColorKeys(null, bundle)).isEmpty();
    }

    @Test
    void resolveSelectedColorKeys_空リストはemptyを返す() {
        assertThat(service.resolveSelectedColorKeys(List.of(), bundle)).isEmpty();
    }

    @Test
    void resolveSelectedColorKeys_一致するcolorIdはキーに変換される() {
        ProductFilterOptionsBundle numericBundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("1", "ブラック", "#000000")),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        assertThat(service.resolveSelectedColorKeys(List.of(1L), numericBundle)).containsExactly("1");
    }

    @Test
    void resolveSelectedColorKeys_bundleに存在しないidは除外される() {
        ProductFilterOptionsBundle numericBundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("1", "ブラック", "#000000")),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        assertThat(service.resolveSelectedColorKeys(List.of(99L), numericBundle)).isEmpty();
    }

    // ---- buildDeskFilter ----

    @Test
    void buildDeskFilter_すべてnullリストのときは全フィールドが空になる() {
        ProductCategoryFilter result = service.buildDeskFilter(null, null, null, null, null, bundle);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void buildDeskFilter_有効な幅レンジIDは通過する() {
        ProductCategoryFilter result = service.buildDeskFilter(null, List.of(1, 11), null, null, null, bundle);
        assertThat(result.deskWidthBandIds()).containsExactly(1, 11);
    }

    @Test
    void buildDeskFilter_許可外の幅レンジIDは除外される() {
        ProductCategoryFilter result = service.buildDeskFilter(null, List.of(99), null, null, null, bundle);
        assertThat(result.deskWidthBandIds()).isEmpty();
    }

    @Test
    void buildDeskFilter_テイストIDはbundleのオプションで正規化される() {
        // bundleのtasteOptionsはid=1,2,3。id=99は除外される
        ProductCategoryFilter result = service.buildDeskFilter(null, null, null, null, List.of(1, 99), bundle);
        assertThat(result.tasteIds()).containsExactly(1L);
    }

    @Test
    void buildDeskFilter_チェア収納フィールドはつねに空になる() {
        ProductCategoryFilter result = service.buildDeskFilter(null, null, null, null, null, bundle);
        assertThat(result.chairFunctionIds()).isEmpty();
        assertThat(result.chairMaterialIds()).isEmpty();
        assertThat(result.storageUsageIds()).isEmpty();
    }

    // ---- buildChairFilter ----

    @Test
    void buildChairFilter_すべてnullリストのときは全フィールドが空になる() {
        ProductCategoryFilter result = service.buildChairFilter(null, null, null, bundle);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void buildChairFilter_テイストIDはbundleのオプションで正規化される() {
        ProductCategoryFilter result = service.buildChairFilter(null, null, List.of(2, 99), bundle);
        assertThat(result.tasteIds()).containsExactly(2L);
    }

    @Test
    void buildChairFilter_デスク収納フィールドはつねに空になる() {
        ProductCategoryFilter result = service.buildChairFilter(null, null, null, bundle);
        assertThat(result.deskTopShapeIds()).isEmpty();
        assertThat(result.deskWidthBandIds()).isEmpty();
        assertThat(result.storageUsageIds()).isEmpty();
    }

    // ---- buildStorageFilter ----

    @Test
    void buildStorageFilter_すべてnullリストのときは全フィールドが空になる() {
        ProductCategoryFilter result = service.buildStorageFilter(null, null, bundle);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void buildStorageFilter_テイストIDはbundleのオプションで正規化される() {
        ProductCategoryFilter result = service.buildStorageFilter(null, List.of(3, 99), bundle);
        assertThat(result.tasteIds()).containsExactly(3L);
    }

    @Test
    void buildStorageFilter_デスクチェアフィールドはつねに空になる() {
        ProductCategoryFilter result = service.buildStorageFilter(null, null, bundle);
        assertThat(result.deskTopShapeIds()).isEmpty();
        assertThat(result.deskWidthBandIds()).isEmpty();
        assertThat(result.chairFunctionIds()).isEmpty();
        assertThat(result.chairMaterialIds()).isEmpty();
    }
}
