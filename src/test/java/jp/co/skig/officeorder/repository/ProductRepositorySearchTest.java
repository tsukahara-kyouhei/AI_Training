package jp.co.skig.officeorder.repository;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductListPage;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductRepository#search} の2パス検索ロジック検証。
 */
@ExtendWith(MockitoExtension.class)
class ProductRepositorySearchTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AppTimeProvider appTimeProvider;

    @InjectMocks
    private ProductRepository repository;

    private ProductSearchCondition conditionWithKeyword(String keyword) {
        return new ProductSearchCondition(
                null, keyword, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null
        );
    }

    private void stubNow() {
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(OffsetDateTime.now());
    }

    // 6-E-1
    @Test
    void singlePassWhenExactMatchFound() {
        stubNow();
        when(productMapper.countProducts(any())).thenReturn(3L);
        when(productMapper.selectProducts(any())).thenReturn(List.of());

        repository.search(conditionWithKeyword("OD-101"));

        // countProducts は1回だけ呼ばれる
        verify(productMapper, times(1)).countProducts(any());
    }

    // 6-E-2
    @Test
    void fallbackToPrefixWhenExactMatchReturnsZero() {
        stubNow();
        // 1パス目(exact)=0、2パス目(prefix)=2
        when(productMapper.countProducts(any()))
                .thenReturn(0L)
                .thenReturn(2L);
        when(productMapper.selectProducts(any())).thenReturn(List.of());

        repository.search(conditionWithKeyword("OD-1"));

        verify(productMapper, times(2)).countProducts(any());
    }

    // 6-E-3
    @Test
    void exactModeParamSetOnFirstPass() {
        stubNow();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        when(productMapper.countProducts(captor.capture())).thenReturn(5L);
        when(productMapper.selectProducts(any())).thenReturn(List.of());

        repository.search(conditionWithKeyword("OD-101"));

        Map<String, Object> params = captor.getValue();
        assertThat(params.get("productCodeMatchMode")).isEqualTo("exact");
    }

    // 6-E-4
    @Test
    void prefixModeParamSetOnSecondPass() {
        stubNow();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        when(productMapper.countProducts(captor.capture()))
                .thenReturn(0L)
                .thenReturn(1L);
        when(productMapper.selectProducts(any())).thenReturn(List.of());

        repository.search(conditionWithKeyword("OD"));

        List<Map<String, Object>> allParams = captor.getAllValues();
        assertThat(allParams.get(0).get("productCodeMatchMode")).isEqualTo("exact");
        assertThat(allParams.get(1).get("productCodeMatchMode")).isEqualTo("prefix");
    }

    // 6-E-5
    @Test
    void noFallbackWhenKeywordIsNull() {
        stubNow();
        when(productMapper.countProducts(any())).thenReturn(0L);

        ProductListPage result = repository.search(conditionWithKeyword(null));

        verify(productMapper, times(1)).countProducts(any());
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    // 6-E-6
    @Test
    void emptyResultWhenBothPassesReturnZero() {
        stubNow();
        when(productMapper.countProducts(any())).thenReturn(0L);

        ProductListPage result = repository.search(conditionWithKeyword("NOMATCH"));

        verify(productMapper, times(2)).countProducts(any());
        verify(productMapper, never()).selectProducts(any());
        assertThat(result.totalCount()).isZero();
    }
}
