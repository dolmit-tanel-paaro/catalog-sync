package ee.paaro.catalogsync.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * The raw payload of a message that referenced something which did not exist
 * yet. Rows are kept after resolution: they are the record of why a placeholder
 * was created, and they are what a replay would read.
 */
@Entity
@Table(name = "staged_messages")
public class StagedMessage {

    public enum Status {
        PARKED,
        RESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "unresolved_key", nullable = false)
    private String unresolvedKey;

    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "received_at", insertable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected StagedMessage() {
    }

    public StagedMessage(String messageId, String externalId, String unresolvedKey, String payload) {
        this.messageId = messageId;
        this.externalId = externalId;
        this.unresolvedKey = unresolvedKey;
        this.payload = payload;
        this.status = Status.PARKED;
    }

    public void resolve() {
        this.status = Status.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getUnresolvedKey() {
        return unresolvedKey;
    }

    public String getPayload() {
        return payload;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
