package jp.co.skig.officeorder.repository;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.ProductMapper;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductCategory;
import jp.co.skig.officeorder.model.product.ProductSearchCondition;
import jp.co.skig.officeorder.model.product.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductRepository#search} が {@link ProductCategoryFilter#tasteIds()} を
 * 正しく SQL パラメータとして渡すことを検証するテスト。
 */
class ProductRepositoryTasteParamsTest {

    private ProductMapper productMapper;
    private AppTimeProvider appTimeProvider;
    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        appTimeProvider = mock(AppTimeProvider.class);
        repository = new ProductRepository(productMapper, appTimeProvider);

        when(appTimeProvider.nowOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(productMapper.countProducts(any())).thenReturn(0L);
        when(productMapper.selectProducts(any())).thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // tasteIds パラメータの格納確認
    // -----------------------------------------------------------------------

    @Test
    void search_deskCategory_tasteIds_putInParams() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(1, 3)
        );
        ProductSearchCondition condition = new ProductSearchCondition(
                ProductCategory.DESK.id(), null, false, List.of(), List.of(),
                filter, ProductSort.RECOMMENDED, 1, 15, null, null
        );

        repository.search(condition);

        ArgumentCaptor<Map<String, Object>> captor = paramCaptor();
        verify(productMapper).countProducts(captor.capture());
        Map<String, Object> params = captor.getValue();

        assertThat(params).containsKey("tasteIds");
        assertThat(params.get("tasteIds")).isEqualTo(List.of(1, 3));
    }

    @Test
    void search_deskCategory_oldTasteKeys_notPresent() {
        ProductSearchCondition condition = minimalCondition(ProductCategory.DESK.id());

        repository.search(condition);

        ArgumentCaptor<Map<String, Object>> captor = paramCaptor();
        verify(productMapper).countProducts(captor.capture());
        Map<String, Object> params = captor.getValue();

        assertThat(params).doesNotContainKey("deskTasteIds");
        assertThat(params).doesNotContainKey("chairTasteIds");
        assertThat(params).doesNotContainKey("storageTasteIds");
    }

    // -----------------------------------------------------------------------
    // hasDeskFilter / hasChairFilter / hasStorageFilter の確認
    // -----------------------------------------------------------------------

    @Test
    void search_deskCategory_withTasteIds_hasDeskFilterTrue() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(1)
        );
        ProductSearchCondition condition = new ProductSearchCondition(
                ProductCategory.DESK.id(), null, false, List.of(), List.of(),
                filter, ProductSort.RECOMMENDED, 1, 15, null, null
        );

        repository.search(condition);

        ArgumentCaptor<Map<String, Object>> captor = paramCaptor();
        verify(productMapper).countProducts(captor.capture());
        Map<String, Object> params = captor.getValue();

        assertThat(params.get("hasDeskFilter")).isEqualTo(Boolean.TRUE);
        assertThat(params.get("hasChairFilter")).isEqualTo(Boolean.FALSE);
        assertThat(params.get("hasStorageFilter")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void search_chairCategory_withTasteIds_hasChairFilterTrue() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(2)
        );
        ProductSearchCondition condition = new ProductSearchCondition(
                ProductCategory.CHAIR.id(), null, false, List.of(), List.of(),
                filter, ProductSort.RECOMMENDED, 1, 15, null, null
        );

        repository.search(condition);

        ArgumentCaptor<Map<String, Object>> captor = paramCaptor();
        verify(productMapper).countProducts(captor.capture());
        Map<String, Object> params = captor.getValue();

        assertThat(params.get("hasDeskFilter")).isEqualTo(Boolean.FALSE);
        assertThat(params.get("hasChairFilter")).isEqualTo(Boolean.TRUE);
        assertThat(params.get("hasStorageFilter")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void search_storageCategory_withTasteIds_hasStorageFilterTrue() {
        ProductCategoryFilter filter = new ProductCategoryFilter(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(3)
        );
        ProductSearchCondition condition = new ProductSearchCondition(
                ProductCategory.STORAGE.id(), null, false, List.of(), List.of(),
                filter, ProductSort.RECOMMENDED, 1, 15, null, null
        );

        repository.search(condition);

        ArgumentCaptor<Map<String, Object>> captor = paramCaptor();
        verify(productMapper).countProducts(captor.capture());
        Map<String, Object> params = captor.getValue();

        assertThat(params.get("hasDeskFilter")).isEqualTo(Boolean.FALSE);
        assertThat(params.get("hasChairFilter")).isEqualTo(Boolean.FALSE);
        assertThat(params.get("hasStorageFilter")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void search_noTasteIds_allAttributeFiltersAreFalse() {
        ProductSearchCondition condition = minimalCondition(ProductCategory.DESK.id());

        repository.search(condition);

        ArgumentCaptor<Map<String, Object>> captor = paramCaptor();
        verify(productMapper).countProducts(captor.capture());
        Map<String, Object> params = captor.getValue();

        assertThat(params.get("hasDeskFilter")).isEqualTo(Boolean.FALSE);
        assertThat(params.get("hasChairFilter")).isEqualTo(Boolean.FALSE);
        assertThat(params.get("hasStorageFilter")).isEqualTo(Boolean.FALSE);
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> paramCaptor() {
        return ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    private ProductSearchCondition minimalCondition(String categoryId) {
        return new ProductSearchCondition(
                categoryId, null, false, List.of(), List.of(),
                ProductCategoryFilter.empty(), ProductSort.RECOMMENDED, 1, 15, null, null
        );
    }
}
