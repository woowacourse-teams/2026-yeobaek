package yeobaek.backend.comment.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

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
    @JoinColumn(name = "sentence_id")
    private Sentence sentence;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "content", nullable = false, length = CommentContent.MAX_LENGTH))
    private CommentContent content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Comment(ClubMember clubMember, Sentence sentence, CommentContent content) {
        this.clubMember = clubMember;
        this.sentence = sentence;
        if (content == null) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public void updateContent(CommentContent content) {
        if (content == null) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(Long memberId) {
        return clubMember.isOwnedBy(memberId);
    }

    public boolean isWriterJoined() {
        return clubMember.isJoined();
    }

    public void ensureReportableBy(Long memberId) {
        if (isWrittenBy(memberId)) {
            throw new BadRequestException(
                    ErrorCode.CANNOT_REPORT_OWN_COMMENT,
                    "본인이 작성한 댓글은 신고할 수 없습니다.");
        }
    }

    public void ensureBookAvailable() {
        clubMember.ensureBookAvailable();
    }

    public String getContent() {
        return content.value();
    }
}
