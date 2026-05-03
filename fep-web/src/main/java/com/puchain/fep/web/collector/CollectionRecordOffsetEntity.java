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
 * advanced by {@link JpaWatermarkStore#put(String, String)} after a successful
 * {@code adapter.acknowledge}.</p>
 *
 * <p>T8-fix: this entity is now the production write path (replaces the prior
 * {@code JdbcTemplate}-based MERGE which did not parse on MySQL 8). Hibernate
 * emits dialect-correct UPSERT semantics for both H2 (MODE=MySQL) and MySQL 8
 * via {@code repository.save()} (INSERT when {@code @Id} row absent, UPDATE
 * otherwise). The future T6b admin UI may surface an "active watermarks" panel
 * via a Spring Data finder on the same repository.</p>
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
