package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One question/answer card with its SM-2 scheduling state. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_FLASHCARDS)
public class Flashcard extends BaseEntity {

    @Column(nullable = false, columnDefinition = "text")
    private String front;

    @Column(nullable = false, columnDefinition = "text")
    private String back;

    @ManyToOne
    @JoinColumn(name = "note_id")
    private LearningNote note;

    @Column(nullable = false)
    private LocalDate dueOn;

    @Column(nullable = false)
    private int intervalDays;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal ease = new BigDecimal("2.50");

    @Column(nullable = false)
    private int repetitions;

    @Column(nullable = false)
    private int lapses;

    private LocalDate lastReviewedOn;
}
