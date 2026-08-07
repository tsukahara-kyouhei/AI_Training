package jp.co.skig.officeorder.service.product;

import java.util.Arrays;
import java.util.List;

import jp.co.skig.officeorder.model.product.CategoryFilterOption;
import jp.co.skig.officeorder.model.product.ColorFilterOption;
import jp.co.skig.officeorder.model.product.ProductCategoryFilter;
import jp.co.skig.officeorder.model.product.ProductFilterOptionsBundle;
import jp.co.skig.officeorder.repository.ProductFilterOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class ProductFilterOptionServiceTest {

    private ProductFilterOptionRepository repository;
    private ProductFilterOptionService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ProductFilterOptionRepository.class);
        service = new ProductFilterOptionService(repository);
    }

    @Test
    void normalizePriceBandIds_filtersInvalidAndDuplicates() {
        assertEquals(List.of(1, 2), service.normalizePriceBandIds(Arrays.asList(1, 2, 2, null, 99)));
    }

    @Test
    void normalizeColorKeys_filtersToAllowedKeys() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("10", "Black", "#000000"),
                        new ColorFilterOption("20", "White", "#ffffff")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals(List.of("10", "20"), service.normalizeColorKeys(List.of("10", "20", "invalid"), bundle));
    }

    @Test
    void resolveColorIds_parsesAllowedColorKeys() {
        ProductFilterOptionsBundle bundle = new ProductFilterOptionsBundle(
                List.of(new ColorFilterOption("10", "Black", "#000000"),
                        new ColorFilterOption("20", "White", "#ffffff")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertEquals(List.of(10L), service.resolveColorIds(List.of("10", "invalid"), bundle));
    }
}
