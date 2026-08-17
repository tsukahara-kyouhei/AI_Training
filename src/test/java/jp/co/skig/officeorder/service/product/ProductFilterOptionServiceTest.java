package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductFilterOptionServiceTest {

    @Mock
    private ProductFilterOptionRepository repository;

    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        service = new ProductFilterOptionService(repository);
    }

    // --- テスト用のダミーオブジェクト生成ヘルパーメソッド ---

    private ColorFilterOption createColorOption(String key, String name, String code) {
        return new ColorFilterOption(key, name, code);
    }

    private CategoryFilterOption createCategoryOption(int id, String name) {
        return new CategoryFilterOption(id, name);
    }

    private void mockRepositoryOptionsBundle() {
        when(repository.findActiveColorOptions()).thenReturn(List.of(
                createColorOption("1", "ブラック", "#000000"),
                createColorOption("2", "ホワイト", "#FFFFFF")
        ));
        when(repository.findActiveDeskTopShapeOptions()).thenReturn(List.of(createCategoryOption(1, "平机")));
        when(repository.findActiveDeskTasteOptions()).thenReturn(List.of(createCategoryOption(10, "シンプル")));
        when(repository.findActiveChairFunctionOptions()).thenReturn(List.of(createCategoryOption(100, "リクライニング")));
        when(repository.findActiveChairMaterialOptions()).thenReturn(List.of(createCategoryOption(101, "メッシュ")));
        when(repository.findActiveChairTasteOptions()).thenReturn(List.of(createCategoryOption(102, "モダン")));
        when(repository.findActiveStorageUsageOptions()).thenReturn(List.of(createCategoryOption(200, "書類整理")));
        when(repository.findActiveStorageTasteOptions()).thenReturn(List.of(createCategoryOption(201, "ナチュラル")));
    }

    @Nested
    @DisplayName("ロード・一覧取得テスト")
    class LoadOptionsTest {

        @Test
        @DisplayName("loadOptionsBundle: 共通のオプション一式が正常に読み込まれること")
        void loadOptionsBundle() {
            mockRepositoryOptionsBundle();

            ProductFilterOptionsBundle bundle = service.loadOptionsBundle();

            assertThat(bundle.colorOptions()).hasSize(2);
            assertThat(bundle.deskTopShapeOptions()).hasSize(1);
            assertThat(bundle.chairFunctionOptions()).hasSize(1);
            assertThat(bundle.storageUsageOptions()).hasSize(1);

            verify(repository).findActiveColorOptions();
            verify(repository).findActiveDeskTopShapeOptions();
            verify(repository).findActiveChairFunctionOptions();
            verify(repository).findActiveStorageUsageOptions();
        }
    }

    @Nested
    @DisplayName("カテゴリ固有フィルター構築テスト")
    class BuildFilterTest {

        @Test
        @DisplayName("buildDeskFilter: 生値から許容範囲内のデータのみが正規化されてセットされること")
        void buildDeskFilter() {
            mockRepositoryOptionsBundle();

            // リポジトリにある deskTopShapeOption(id: 1), deskTasteOption(id: 10)
            // デスク幅の許容レンジID(1〜11)
            ProductCategoryFilter filter = service.buildDeskFilter(
                    List.of(1, 999),          // 天板形状（999は不正値）
                    List.of(1, 12),           // 幅レンジ（12は許容外）
                    List.of(2),               // 奥行レンジ
                    List.of(3),               // 高さレンジ
                    List.of(10, 888)          // テイスト（888は不正値）
            );

            assertThat(filter.deskTopShapeIds()).containsExactly(1);
            assertThat(filter.deskWidthBandIds()).containsExactly(1);
            assertThat(filter.deskDepthBandIds()).containsExactly(2);
            assertThat(filter.deskHeightBandIds()).containsExactly(3);
            assertThat(filter.deskTasteIds()).containsExactly(10);
            assertThat(filter.chairFunctionIds()).isEmpty();
        }

        @Test
        @DisplayName("buildChairFilter: チェア向けの入力値が正規化されること")
        void buildChairFilter() {
            mockRepositoryOptionsBundle();

            ProductCategoryFilter filter = service.buildChairFilter(
                    List.of(100), // 機能
                    List.of(101), // 素材
                    List.of(102)  // テイスト
            );

            assertThat(filter.chairFunctionIds()).containsExactly(100);
            assertThat(filter.chairMaterialIds()).containsExactly(101);
            assertThat(filter.chairTasteIds()).containsExactly(102);
            assertThat(filter.deskTopShapeIds()).isEmpty();
        }

        @Test
        @DisplayName("buildStorageFilter: 収納家具向けの入力値が正規化されること")
        void buildStorageFilter() {
            mockRepositoryOptionsBundle();

            ProductCategoryFilter filter = service.buildStorageFilter(
                    List.of(200), // 用途
                    List.of(201)  // テイスト
            );

            assertThat(filter.storageUsageIds()).containsExactly(200);
            assertThat(filter.storageTasteIds()).containsExactly(201);
        }
    }

    @Nested
    @DisplayName("価格帯・カラー正規化テスト")
    class NormalizationTest {

        @Test
        @DisplayName("normalizePriceBandIds: 不正なIDや重複が排除されること")
        void normalizePriceBandIds() {
            // PriceBandに含まれるID（例: 1, 2）と含まれない不正値（-1, 999）
            List<Integer> rawIds = List.of(1, 2, 2, 999);

            List<Integer> normalized = service.normalizePriceBandIds(rawIds);

            // 重複と不正値が除外されること
            assertThat(normalized).containsExactly(1, 2);
        }

        @Test
        @DisplayName("normalizeColorKeys: 有効なカラーキーのみが抽出されること")
        void normalizeColorKeys() {
            mockRepositoryOptionsBundle(); // "1", "2" が有効

            List<String> rawKeys = List.of("1", "invalid_key", "1");

            List<String> normalized = service.normalizeColorKeys(rawKeys);

            assertThat(normalized).containsExactly("1");
        }

        @Test
        @DisplayName("resolveColorIds: カラーキーからLong型のID一覧へ変換されること")
        void resolveColorIds() {
            mockRepositoryOptionsBundle(); // "1", "2" が有効

            List<String> colorKeys = List.of("1", "2", "abc");

            List<Long> colorIds = service.resolveColorIds(colorKeys);

            assertThat(colorIds).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("resolveSelectedColorKeys: 選択されたIDから画面表示順のカラーキーに復元されること")
        void resolveSelectedColorKeys() {
            mockRepositoryOptionsBundle(); // 順序: "1", "2"

            List<Long> selectedIds = List.of(2L, 1L); // 逆順で入力

            List<String> resolvedKeys = service.resolveSelectedColorKeys(selectedIds);

            // マスタの定義順（"1", "2"）で復元されること
            assertThat(resolvedKeys).containsExactly("1", "2");
        }

        @Test
        @DisplayName("nullまたは空のリスト入力時に空リストが返されること")
        void handleNullOrEmpty() {
            assertThat(service.normalizePriceBandIds(null)).isEmpty();
            assertThat(service.normalizeColorKeys(Collections.emptyList())).isEmpty();
            assertThat(service.resolveColorIds(null)).isEmpty();
            assertThat(service.resolveSelectedColorKeys(null)).isEmpty();
        }
    }
}