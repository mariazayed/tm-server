package trademind.tm_server.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Abstract base entity class that provides common fields for all entities in the system.
 * All domain entities should extend this class to inherit standard fields like ID and timestamps.
 *
 * <p>Includes:
 * <ul>
 *   <li>Auto-generated unique identifier</li>
 *   <li>Audit timestamps (creation and update)</li>
 *   <li>Soft delete support via deletedAt timestamp</li>
 * </ul>
 *
 * @see MappedSuperclass
 */
@Getter
@Setter
@MappedSuperclass
@Schema(description = "Base entity containing common fields inherited by all entities in the system")
public abstract class BaseEntity {

    @Schema(
            description = "Unique identifier of the entity, auto-generated on creation",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(
            description = "Timestamp when the entity was created, automatically set on first save",
            example = "2024-01-15T10:15:30",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the entity was last updated, automatically refreshed on each save",
            example = "2024-01-20T14:20:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Schema(
            description = "Timestamp when the entity was soft deleted. Null indicates the entity is active. Non-null indicates the entity has been deleted but retained in the database for audit purposes.",
            example = "2024-01-25T16:45:00"
    )
    private LocalDateTime deletedAt;

    private String notes;
}