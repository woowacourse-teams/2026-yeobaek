package yeobaek.backend.comment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appreciation_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uk_appreciation_reports_reporter_appreciation",
                columnNames = {"reporter_id", "appreciation_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private Long reporterId;

    @Column(name = "appreciation_id", nullable = false, updatable = false)
    private Long appreciationId;

    public CommentReport(Long reporterId, Long appreciationId) {
        this.reporterId = reporterId;
        this.appreciationId = appreciationId;
    }
}
