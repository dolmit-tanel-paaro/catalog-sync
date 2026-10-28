package ee.paaro.catalogsync.service;

import ee.paaro.catalogsync.TestcontainersConfiguration;
import ee.paaro.catalogsync.domain.Category;
import ee.paaro.catalogsync.domain.Product;
import ee.paaro.catalogsync.domain.StagedMessage;
import ee.paaro.catalogsync.messaging.CategoryMessage;
import ee.paaro.catalogsync.messaging.ProductMessage;
import ee.paaro.catalogsync.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogueImportServiceIT {

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
    @DisplayName("category first, then its product: no placeholder is involved")
    void inOrder() {
        service.apply(new CategoryMessage("m1", "CAT-1", "Rings"));

        ImportOutcome outcome = service.apply(product("m2", "SKU-1", "CAT-1"));

        assertThat(outcome).isEqualTo(ImportOutcome.APPLIED);
        assertThat(staged.count()).isZero();
        assertThat(categories.findByExternalId("CAT-1")).get()
                .extracting(Category::isPlaceholder).isEqualTo(false);
    }

    @Test
    @DisplayName("product arrives first: it is stored against a placeholder and the payload is parked")
    void productBeforeCategory() {
        ImportOutcome outcome = service.apply(product("m1", "SKU-1", "CAT-1"));

        assertThat(outcome).isEqualTo(ImportOutcome.APPLIED_WITH_PLACEHOLDER);

        Category category = categories.findByExternalId("CAT-1").orElseThrow();
        assertThat(category.isPlaceholder()).isTrue();
        assertThat(products.findByExternalId("SKU-1")).get()
                .extracting(p -> p.getCategory().getId()).isEqualTo(category.getId());

        assertThat(staged.findAllByStatusOrderByReceivedAtAsc(StagedMessage.Status.PARKED))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.getUnresolvedKey()).isEqualTo("CAT-1");
                    assertThat(m.getExternalId()).isEqualTo("SKU-1");
                    assertThat(m.getPayload()).contains("SKU-1");
                });
    }

    @Test
    @DisplayName("the real category keeps the placeholder's row, so nothing has to be re-linked")
    void placeholderIsPromotedInPlace() {
        service.apply(product("m1", "SKU-1", "CAT-1"));
        Long placeholderId = categories.findByExternalId("CAT-1").orElseThrow().getId();

        ImportOutcome outcome = service.apply(new CategoryMessage("m2", "CAT-1", "Rings"));

        assertThat(outcome).isEqualTo(ImportOutcome.PLACEHOLDER_PROMOTED);

        Category promoted = categories.findByExternalId("CAT-1").orElseThrow();
        assertThat(promoted.getId()).isEqualTo(placeholderId);
        assertThat(promoted.isPlaceholder()).isFalse();
        assertThat(promoted.getName()).isEqualTo("Rings");

        assertThat(products.findByExternalId("SKU-1")).get()
                .extracting(p -> p.getCategory().getId()).isEqualTo(placeholderId);

        assertThat(staged.countByStatus(StagedMessage.Status.PARKED)).isZero();
        assertThat(staged.countByStatus(StagedMessage.Status.RESOLVED)).isEqualTo(1);
    }

    @Test
    @DisplayName("redelivery of the same message changes nothing")
    void redeliveryIsIgnored() {
        service.apply(new CategoryMessage("m1", "CAT-1", "Rings"));
        service.apply(product("m2", "SKU-1", "CAT-1"));

        ImportOutcome outcome = service.apply(product("m2", "SKU-1", "CAT-1"));

        assertThat(outcome).isEqualTo(ImportOutcome.DUPLICATE);
        assertThat(products.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a later message for the same product updates it rather than duplicating it")
    void secondMessageUpdatesProduct() {
        service.apply(new CategoryMessage("m1", "CAT-1", "Rings"));
        service.apply(product("m2", "SKU-1", "CAT-1"));

        service.apply(new ProductMessage("m3", "SKU-1", "Renamed", new BigDecimal("12.50"), "CAT-1"));

        assertThat(products.count()).isEqualTo(1);
        assertThat(products.findByExternalId("SKU-1")).get()
                .extracting(Product::getName).isEqualTo("Renamed");
    }

    @Test
    @DisplayName("the back office can move a product off a placeholder by hand")
    void manualMove() {
        service.apply(product("m1", "SKU-1", "CAT-MISSING"));
        service.apply(new CategoryMessage("m2", "CAT-REAL", "Rings"));

        service.moveProduct("SKU-1", "CAT-REAL");

        Long realId = categories.findByExternalId("CAT-REAL").orElseThrow().getId();
        assertThat(products.findByExternalId("SKU-1")).get()
                .extracting(p -> p.getCategory().getId()).isEqualTo(realId);
    }

    @Test
    @DisplayName("moving to a category that does not exist is rejected")
    void manualMoveToUnknownCategory() {
        service.apply(product("m1", "SKU-1", "CAT-MISSING"));

        assertThatThrownBy(() -> service.moveProduct("SKU-1", "NOPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProductMessage product(String messageId, String sku, String categoryId) {
        return new ProductMessage(messageId, sku, "Solitaire", new BigDecimal("499.00"), categoryId);
    }
}
