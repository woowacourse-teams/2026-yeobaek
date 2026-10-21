package yeobaek.backend.comment.domain;

import static yeobaek.backend.support.LogField.COMMENT_ID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.publicroom.domain.PublicRoom;
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

    @OneToOne(mappedBy = "comment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private ClubComment clubComment;

    @OneToOne(mappedBy = "comment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private PublicRoomComment publicRoomComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private Member writer;

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
        if (clubMember == null) {
            throw new IllegalArgumentException("모임 회원은 필수입니다.");
        }
        this.writer = clubMember.getMember();
        this.clubComment = new ClubComment(this, clubMember.getClub());
        this.sentence = sentence;
        setContentOnCreate(content);
    }

    public Comment(PublicRoom publicRoom, Member writer, Sentence sentence, CommentContent content) {
        if (publicRoom == null) {
            throw new IllegalArgumentException("공개방은 필수입니다.");
        }
        this.writer = writer;
        this.publicRoomComment = new PublicRoomComment(this, publicRoom);
        this.sentence = sentence;
        setContentOnCreate(content);
    }

    private void setContentOnCreate(CommentContent content) {
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
        return getWriter().getId().equals(memberId);
    }

    public boolean isWrittenByAny(Set<Long> memberIds) {
        return memberIds.contains(getWriter().getId());
    }

    public boolean isViewed(Set<Long> viewedCommentIds) {
        return viewedCommentIds.contains(id);
    }

    public Long getSentenceId() {
        return sentence.getId();
    }

    public void ensureReportableBy(Long memberId) {
        if (isWrittenBy(memberId)) {
            throw new BadRequestException(
                    ErrorCode.CANNOT_REPORT_OWN_COMMENT,
                    "본인이 작성한 댓글은 신고할 수 없습니다.",
                    id == null ? Map.of() : Map.of(COMMENT_ID, id.toString()));
        }
    }

    public void ensureBookAvailable() {
        if (publicRoomComment != null) {
            publicRoomComment.getPublicRoom().ensureBookAvailable();
            return;
        }
        clubComment.getClub().ensureBookAvailable();
    }

    public boolean isPublicRoomComment() {
        return publicRoomComment != null;
    }

    public Member getWriter() {
        return writer;
    }

    public Long getClubId() {
        return clubComment == null ? null : clubComment.getClub().getId();
    }

    public String getContent() {
        return content.value();
    }
}
