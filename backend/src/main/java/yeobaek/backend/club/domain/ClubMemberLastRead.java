package yeobaek.backend.club.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import yeobaek.backend.book.domain.Passage;

@Entity
@Table(name = "club_member_last_reads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_club_member_last_reads_member", columnNames = "club_member_id")
})
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMemberLastRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_member_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ClubMember clubMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_passage_id")
    private Passage lastReadPassage;

    @Column
    private LocalDateTime lastReadAt;

    ClubMemberLastRead(ClubMember clubMember, Passage lastReadPassage, LocalDateTime lastReadAt) {
        this.clubMember = clubMember;
        this.lastReadPassage = lastReadPassage;
        this.lastReadAt = lastReadAt;
    }

    void update(Passage passage, LocalDateTime readAt) {
        this.lastReadPassage = passage;
        this.lastReadAt = readAt;
    }
}
