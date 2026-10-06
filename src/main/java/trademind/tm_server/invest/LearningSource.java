package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.enums.SourceType;

/** A book, video, article or podcast on the reading/watch list. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_SOURCES)
public class LearningSource extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(length = 2048)
    private String url;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private SourceStatus status = SourceStatus.TO_DO;
}
