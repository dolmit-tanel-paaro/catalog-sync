package ee.paaro.catalogsync.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    /**
     * True while this row only exists because a product referenced it before the
     * category itself had arrived. The row is real enough to hang products on,
     * but it is flagged so the back office can show it for manual placement.
     */
    @Column(nullable = false)
    private boolean placeholder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Category() {
    }

    private Category(String externalId, String name, boolean placeholder) {
        this.externalId = externalId;
        this.name = name;
        this.placeholder = placeholder;
        this.updatedAt = Instant.now();
    }

    public static Category real(String externalId, String name) {
        return new Category(externalId, name, false);
    }

    public static Category placeholderFor(String externalId) {
        return new Category(externalId, "Unplaced (" + externalId + ")", true);
    }

    /**
     * The category has finally arrived from the ERP. Keep the row and its id, so
     * every product already pointing at it stays pointed at it.
     */
    public void promote(String realName) {
        this.name = realName;
        this.placeholder = false;
        this.updatedAt = Instant.now();
    }

    public void rename(String newName) {
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
