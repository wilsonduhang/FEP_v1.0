package com.puchain.fep.web.collector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the {@code collection_record_offset} table created by Flyway
 * migration {@code V23__create_collection_run.sql} (P4 T8).
 *
 * <p>One row per {@code adapterId}; {@code watermark} is the opaque cursor
 * advanced by {@link JdbcWatermarkStore#put(String, String)} after a successful
 * {@code adapter.acknowledge}.</p>
 *
 * <p>This entity is not used by {@link JdbcWatermarkStore} (which uses
 * {@code JdbcTemplate} directly to avoid an entity-graph round-trip per
 * watermark write); it exists for two reasons:
 * <ul>
 *   <li>{@code @DataJpaTest} schema-validation can assert the V23 mapping is
 *       discoverable / correctly typed.</li>
 *   <li>Future T6b admin UI may surface an "active watermarks" panel via a
 *       Spring Data finder without needing a second access path.</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "collection_record_offset")
public class CollectionRecordOffsetEntity {

    @Id
    @Column(name = "adapter_id", nullable = false, length = 64)
    private String adapterId;

    @Column(name = "watermark", nullable = false, length = 128)
    private String watermark;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public CollectionRecordOffsetEntity() {
        // JPA
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(final String adapterId) {
        this.adapterId = adapterId;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(final String watermark) {
        this.watermark = watermark;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
