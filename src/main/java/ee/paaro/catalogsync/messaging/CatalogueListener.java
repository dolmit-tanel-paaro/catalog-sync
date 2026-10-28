package ee.paaro.catalogsync.messaging;

import ee.paaro.catalogsync.service.CatalogueImportService;
import ee.paaro.catalogsync.service.ImportOutcome;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class CatalogueListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogueListener.class);

    private final CatalogueImportService importService;

    public CatalogueListener(CatalogueImportService importService) {
        this.importService = importService;
    }

    @RabbitListener(queues = RabbitConfig.PRODUCT_QUEUE)
    public void onProduct(@Valid ProductMessage message) {
        ImportOutcome outcome = importService.apply(message);
        log.debug("Product {} -> {}", message.externalId(), outcome);
    }

    @RabbitListener(queues = RabbitConfig.CATEGORY_QUEUE)
    public void onCategory(@Valid CategoryMessage message) {
        ImportOutcome outcome = importService.apply(message);
        log.debug("Category {} -> {}", message.externalId(), outcome);
    }
}
