package ee.paaro.catalogsync.web;

import ee.paaro.catalogsync.TestcontainersConfiguration;
import ee.paaro.catalogsync.messaging.CategoryMessage;
import ee.paaro.catalogsync.messaging.ProductMessage;
import ee.paaro.catalogsync.repository.*;
import ee.paaro.catalogsync.service.CatalogueImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PlaceholderControllerIT {

    @Autowired
    PlaceholderController controller;
    @Autowired
    CatalogueImportService service;
    @Autowired
    CategoryRepository categories;
    @Autowired
    ProductRepository products;
    @Autowired
    StagedMessageRepository staged;
    @Autowired
    ProcessedMessageRepository processed;

    @BeforeEach
    void clean() {
        products.deleteAllInBatch();
        categories.deleteAllInBatch();
        staged.deleteAllInBatch();
        processed.deleteAllInBatch();
    }

    @Test
    @DisplayName("nothing parked: the listing is empty")
    void noPlaceholders() {
        service.apply(new CategoryMessage("m1", "CAT-1", "Rings"));
        service.apply(product("m2", "SKU-1", "CAT-1"));

        assertThat(controller.list()).isEmpty();
    }

    @Test
    @DisplayName("several placeholders: each carries its own products and no others")
    void groupsProductsByPlaceholder() {
        service.apply(product("m1", "SKU-1", "CAT-MISSING-A"));
        service.apply(product("m2", "SKU-2", "CAT-MISSING-A"));
        service.apply(product("m3", "SKU-3", "CAT-MISSING-B"));

        List<PlaceholderController.PlaceholderView> view = controller.list();

        assertThat(view).extracting(PlaceholderController.PlaceholderView::externalId)
                .containsExactlyInAnyOrder("CAT-MISSING-A", "CAT-MISSING-B");
        assertThat(productsUnder(view, "CAT-MISSING-A")).containsExactlyInAnyOrder("SKU-1", "SKU-2");
        assertThat(productsUnder(view, "CAT-MISSING-B")).containsExactly("SKU-3");
    }

    @Test
    @DisplayName("a placeholder with nothing under it still lists, with an empty product list")
    void placeholderWithoutProducts() {
        service.apply(product("m1", "SKU-1", "CAT-MISSING"));
        products.deleteAllInBatch();

        List<PlaceholderController.PlaceholderView> view = controller.list();

        assertThat(view).hasSize(1);
        assertThat(view.getFirst().productExternalIds()).isEmpty();
    }

    private static List<String> productsUnder(List<PlaceholderController.PlaceholderView> view, String externalId) {
        return view.stream()
                .filter(v -> v.externalId().equals(externalId))
                .findFirst()
                .orElseThrow()
                .productExternalIds();
    }

    private ProductMessage product(String messageId, String sku, String categoryExternalId) {
        return new ProductMessage(messageId, sku, "Product " + sku, new BigDecimal("10.00"), categoryExternalId);
    }
}
