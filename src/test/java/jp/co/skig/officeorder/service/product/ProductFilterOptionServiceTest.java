package jp.co.skig.officeorder.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.PriceBand;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 商品絞り込み候補サービスの単体テスト。
 *
 * <p>単体テスト計画の PRD-008（不正な候補IDの除外）に対応する。
 */
class ProductFilterOptionServiceTest {

    private ProductFilterOptionRepository repository;
    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductFilterOptionRepository.class);
        service = new ProductFilterOptionService(repository);
    }

    private ProductFilterOptionsBundle bundle() {
        return new ProductFilterOptionsBundle(
                List.of(
                        new ColorFilterOption("1", "ブラック", "#000000"),
                        new ColorFilterOption("2", "ホワイト", "#FFFFFF")
                ),
                List.of(new CategoryFilterOption(10, "長方形")),
                List.of(new CategoryFilterOption(20, "モダン")),
                List.of(new CategoryFilterOption(30, "リクライニング")),
                List.of(new CategoryFilterOption(40, "ファブリック")),
                List.of(new CategoryFilterOption(50, "クラシック")),
                List.of(new CategoryFilterOption(60, "書類収納")),
                List.of(new CategoryFilterOption(70, "ナチュラル"))
        );
    }

    @Nested
    @DisplayName("PRD-008 不正な候補IDの除外")
    class InvalidCandidateIds {

        @Test
        @DisplayName("価格帯IDは不正値を除外して重複を除去する")
        void normalizePriceBandIds_filtersInvalidAndDeduplicates() {
            assertThat(service.normalizePriceBandIds(Arrays.asList(1, 99, 6, null, 1)))
                    .containsExactly(1, 6);
            assertThat(service.normalizePriceBandIds(null)).isEmpty();
            assertThat(service.normalizePriceBandIds(List.of())).isEmpty();
        }

        @Test
        @DisplayName("カラーキーは候補一覧にないキーを除外する")
        void normalizeColorKeys_filtersUnknownKeys() {
            ProductFilterOptionsBundle options = bundle();

            assertThat(service.normalizeColorKeys(Arrays.asList("1", "999", null, "2"), options))
                    .containsExactly("1", "2");
            assertThat(service.normalizeColorKeys(null, options)).isEmpty();
        }

        @Test
        @DisplayName("カラーキーは候補一覧にないキーを除外してcolor_idへ変換する")
        void resolveColorIds_filtersUnknownKeysAndConverts() {
            ProductFilterOptionsBundle options = bundle();

            assertThat(service.resolveColorIds(Arrays.asList("1", "999", "2"), options))
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("カテゴリ固有フィルタは候補にない天板形状・テイストIDを除外する")
        void buildDeskFilter_filtersUnknownOptionIds() {
            ProductFilterOptionsBundle options = bundle();

            ProductCategoryFilter filter = service.buildDeskFilter(
                    Arrays.asList(10, 999, null),
                    Arrays.asList(1, 99, 2),
                    Arrays.asList(999),
                    Arrays.asList(),
                    Arrays.asList(20, 999),
                    options
            );

            assertThat(filter.deskTopShapeIds()).containsExactly(10);
            assertThat(filter.deskWidthBandIds()).containsExactly(1, 2);
            assertThat(filter.deskDepthBandIds()).isEmpty();
            assertThat(filter.deskHeightBandIds()).isEmpty();
            assertThat(filter.deskTasteIds()).containsExactly(20);
            assertThat(filter.chairFunctionIds()).isEmpty();
            assertThat(filter.storageUsageIds()).isEmpty();
        }

        @Test
        @DisplayName("チェア固有フィルタは候補にない機能・素材・テイストIDを除外する")
        void buildChairFilter_filtersUnknownOptionIds() {
            ProductFilterOptionsBundle options = bundle();

            ProductCategoryFilter filter = service.buildChairFilter(
                    Arrays.asList(30, 999),
                    Arrays.asList(40, 999),
                    Arrays.asList(50, 999),
                    options
            );

            assertThat(filter.chairFunctionIds()).containsExactly(30);
            assertThat(filter.chairMaterialIds()).containsExactly(40);
            assertThat(filter.chairTasteIds()).containsExactly(50);
        }

        @Test
        @DisplayName("収納家具固有フィルタは候補にない用途・テイストIDを除外する")
        void buildStorageFilter_filtersUnknownOptionIds() {
            ProductFilterOptionsBundle options = bundle();

            ProductCategoryFilter filter = service.buildStorageFilter(
                    Arrays.asList(60, 999),
                    Arrays.asList(70, 999),
                    options
            );

            assertThat(filter.storageUsageIds()).containsExactly(60);
            assertThat(filter.storageTasteIds()).containsExactly(70);
        }
    }

    @Nested
    @DisplayName("PRD-006 有効な価格帯だけを反映する")
    class PriceBandReflection {

        @Test
        @DisplayName("不正な価格帯IDはPriceBandへ変換できない")
        void invalidPriceBandId_returnsNull() {
            assertThat(PriceBand.fromId(999)).isNull();
            assertThat(PriceBand.fromId(1)).isEqualTo(PriceBand.BAND_1);
        }
    }
}