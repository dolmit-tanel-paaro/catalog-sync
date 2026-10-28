package ee.paaro.catalogsync.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One row per message the consumer has already applied. RabbitMQ guarantees
 * at-least-once delivery, so the "at most once" half has to live here.
 */
@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "processed_at", insertable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedMessage() {
    }

    public ProcessedMessage(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
