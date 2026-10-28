package ee.paaro.catalogsync.messaging;

import jakarta.validation.constraints.NotBlank;

public record CategoryMessage(
        @NotBlank String messageId,
        @NotBlank String externalId,
        @NotBlank String name
) {
}
