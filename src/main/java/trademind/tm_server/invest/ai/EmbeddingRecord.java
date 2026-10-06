package trademind.tm_server.invest.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import trademind.tm_server.constants.AppConstants;

import java.time.LocalDateTime;

/**
 * A stored embedding of one journal entry or note. Not used by any feature yet; it exists so the table
 * shape is settled and tested before AI search is built on it.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_EMBEDDINGS)
public class EmbeddingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private EmbeddingSource sourceType;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false)
    private int dimensions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "real[]")
    private float[] vector;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
