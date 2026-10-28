package ee.paaro.catalogsync.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductMessage(
        @NotBlank String messageId,
        @NotBlank String externalId,
        @NotBlank String name,
        @PositiveOrZero BigDecimal price,
        @NotBlank String categoryExternalId
) {
}
