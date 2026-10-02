package yeobaek.backend.comment.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.comment.domain.vo.CommentContent;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    private Long id;

    @Column(name = "appreciation_kind", nullable = false, length = 64, updatable = false)
    private String appreciationKind;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "content", nullable = false, length = CommentContent.MAX_LENGTH))
    private CommentContent content;

    public Comment(Long appreciationId, CommentContent content) {
        if (content == null) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        if (appreciationId == null) {
            throw new IllegalArgumentException("댓글 감상 ID는 필수입니다.");
        }
        this.id = appreciationId;
        this.content = content;
        this.appreciationKind = yeobaek.backend.appreciation.domain.Comment.COMMENT_KIND;
    }

    public void updateContent(CommentContent content) {
        if (content == null) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        this.content = content;
    }

    public String getContent() {
        return content.value();
    }

}
