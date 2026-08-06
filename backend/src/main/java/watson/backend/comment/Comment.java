package watson.backend.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import watson.backend.book.Passage;
import watson.backend.club.ClubMember;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_member_id")
    private ClubMember clubMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passage_id")
    private Passage passage;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private static final int MAX_CONTENT_LENGTH = 1000;

    public Comment(ClubMember clubMember, Passage passage, String content) {
        validateContent(content);
        this.clubMember = clubMember;
        this.passage = passage;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public void updateContent(String content) {
        validateContent(content);
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(Long memberId) {
        return clubMember.getMember().getId().equals(memberId);
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("댓글 내용은 공백이 아닌 1~" + MAX_CONTENT_LENGTH + "자여야 합니다.");
        }
    }
}
