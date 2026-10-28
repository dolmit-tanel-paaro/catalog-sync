package ee.paaro.catalogsync.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ee.paaro.catalogsync.domain.*;
import ee.paaro.catalogsync.messaging.CategoryMessage;
import ee.paaro.catalogsync.messaging.ProductMessage;
import ee.paaro.catalogsync.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Applies catalogue messages from the ERP.
 * <p>
 * The ERP does not promise order. A product can arrive before the category it
 * belongs to, and on a first full export it usually does. Rather than rejecting
 * the product or retrying until the category turns up, the product is stored
 * against a placeholder category created on the spot, and the raw payload is
 * parked in {@code staged_messages}. When the real category arrives, the
 * placeholder is filled in rather than replaced, so every product already
 * pointing at it stays pointed at it, and nothing has to be re-linked.
 * <p>
 * A placeholder is also a visible thing, not a hidden queue: the back office
 * lists them so somebody can place a product by hand when the ERP never sends
 * the category at all.
 */
@Service
public class CatalogueImportService {

    private static final Logger log = LoggerFactory.getLogger(CatalogueImportService.class);

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final StagedMessageRepository staged;
    private final ProcessedMessageRepository processed;
    private final ObjectMapper objectMapper;

    public CatalogueImportService(CategoryRepository categories,
                                  ProductRepository products,
                                  StagedMessageRepository staged,
                                  ProcessedMessageRepository processed,
                                  ObjectMapper objectMapper) {
        this.categories = categories;
        this.products = products;
        this.staged = staged;
        this.processed = processed;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportOutcome apply(ProductMessage message) {
        if (alreadyApplied(message.messageId())) {
            return ImportOutcome.DUPLICATE;
        }

        boolean placeholderUsed = false;
        Category category = categories.findByExternalId(message.categoryExternalId()).orElse(null);

        if (category == null) {
            category = categories.save(Category.placeholderFor(message.categoryExternalId()));
            staged.save(new StagedMessage(
                    message.messageId(),
                    message.externalId(),
                    message.categoryExternalId(),
                    serialise(message)));
            placeholderUsed = true;
            log.info("Product {} referenced unknown category {}; parked against a placeholder",
                    message.externalId(), message.categoryExternalId());
        }

        Product product = products.findByExternalId(message.externalId()).orElse(null);
        if (product == null) {
            products.save(new Product(message.externalId(), message.name(), message.price(), category));
        } else {
            product.update(message.name(), message.price(), category);
        }

        markApplied(message.messageId());
        return placeholderUsed ? ImportOutcome.APPLIED_WITH_PLACEHOLDER : ImportOutcome.APPLIED;
    }

    @Transactional
    public ImportOutcome apply(CategoryMessage message) {
        if (alreadyApplied(message.messageId())) {
            return ImportOutcome.DUPLICATE;
        }

        Category existing = categories.findByExternalId(message.externalId()).orElse(null);
        ImportOutcome outcome;

        if (existing == null) {
            categories.save(Category.real(message.externalId(), message.name()));
            outcome = ImportOutcome.APPLIED;
        } else if (existing.isPlaceholder()) {
            existing.promote(message.name());
            releaseParked(message.externalId());
            outcome = ImportOutcome.PLACEHOLDER_PROMOTED;
        } else {
            existing.rename(message.name());
            outcome = ImportOutcome.APPLIED;
        }

        markApplied(message.messageId());
        return outcome;
    }

    /**
     * The manual path: somebody in the back office moves a product off a
     * placeholder because the ERP is never going to send that category.
     */
    @Transactional
    public void moveProduct(String productExternalId, String categoryExternalId) {
        Product product = products.findByExternalId(productExternalId)
                .orElseThrow(() -> new IllegalArgumentException("No product " + productExternalId));
        Category category = categories.findByExternalId(categoryExternalId)
                .orElseThrow(() -> new IllegalArgumentException("No category " + categoryExternalId));

        product.moveTo(category);
    }

    private void releaseParked(String categoryExternalId) {
        List<StagedMessage> parked =
                staged.findAllByUnresolvedKeyAndStatus(categoryExternalId, StagedMessage.Status.PARKED);
        parked.forEach(StagedMessage::resolve);

        if (!parked.isEmpty()) {
            log.info("Category {} arrived; released {} parked message(s)", categoryExternalId, parked.size());
        }
    }

    private boolean alreadyApplied(String messageId) {
        return processed.existsById(messageId);
    }

    private void markApplied(String messageId) {
        processed.save(new ProcessedMessage(messageId));
    }

    private String serialise(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot serialise message for staging", e);
        }
    }
}
