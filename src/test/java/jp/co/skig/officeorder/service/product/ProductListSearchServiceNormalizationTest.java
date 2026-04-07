package jp.co.skig.officeorder.service.product;

import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductListSearchService} のキーワード正規化検証。
 */
@ExtendWith(MockitoExtension.class)
class ProductListSearchServiceNormalizationTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductFilterOptionService productFilterOptionService;

    @InjectMocks
    private ProductListSearchService service;

    private ProductFilterOptionsBundle emptyBundle() {
        return new ProductFilterOptionsBundle(
                null, null, null, null, null, null, null, null, null
        );
    }

    // 6-F-1
    @Test
    void fullWidthKeywordNormalizedBeforePassingToCondition() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, "abc", false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        service.buildCondition(null, "ＡＢＣ", false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isEqualTo("abc");
    }

    // 6-F-2
    @Test
    void halfWidthKatakanaKeywordNormalized() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, "アイウ", false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        service.buildCondition(null, "ｱｲｳ", false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isEqualTo("アイウ");
    }

    // 6-F-3
    @Test
    void nullKeywordPassedAsNullAfterNormalization() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, null, false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        service.buildCondition(null, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isNull();
    }

    // 6-F-4: 半角英字小文字はそのまま小文字として渡される
    @Test
    void halfWidthLowercaseEnglishPassedUnchanged() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, "abc", false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        service.buildCondition(null, "abc", false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isEqualTo("abc");
    }

    // 6-F-5: 半角英字大文字は小文字に変換して渡される
    @Test
    void halfWidthUppercaseEnglishLowercased() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, "abc", false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        service.buildCondition(null, "ABC", false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isEqualTo("abc");
    }

    // 6-F-6: 半角カタカナ+濁点（2文字）は全角カタカナ濁音（1文字）に変換して渡される
    @Test
    void halfWidthKatakanaWithDakutenNormalized() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, "ガギグ", false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        // ｶﾞ→ガ, ｷﾞ→ギ, ｸﾞ→グ
        service.buildCondition(null, "ｶﾞｷﾞｸﾞ", false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isEqualTo("ガギグ");
    }

    // 6-F-7: 半角カタカナ+半濁点（2文字）は全角カタカナ半濁音（1文字）に変換して渡される
    @Test
    void halfWidthKatakanaWithHandakutenNormalized() {
        ProductFilterOptionsBundle bundle = emptyBundle();
        when(productFilterOptionService.normalizePriceBandIds(any())).thenReturn(List.of());
        when(productFilterOptionService.normalizeColorKeys(any(), any())).thenReturn(List.of());
        when(productFilterOptionService.resolveColorIds(any(), any())).thenReturn(List.of());
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.buildCondition(any(), keywordCaptor.capture(), any(boolean.class),
                any(), any(), any(), any(int.class), any(int.class), any(), any()))
                .thenReturn(new ProductSearchCondition(
                        null, "パピプ", false, List.of(), List.of(),
                        ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
                ));

        // ﾊﾟ→パ, ﾋﾟ→ピ, ﾌﾟ→プ
        service.buildCondition(null, "ﾊﾟﾋﾟﾌﾟ", false, List.of(), List.of(),
                ProductCategoryFilter.empty(), null, 1, 15, ProductSort.RECOMMENDED, bundle);

        assertThat(keywordCaptor.getValue()).isEqualTo("パピプ");
    }
}
