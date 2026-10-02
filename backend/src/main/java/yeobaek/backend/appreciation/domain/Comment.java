package yeobaek.backend.appreciation.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

public final class Comment implements Appreciation {

    public static final String COMMENT_KIND = "COMMENT";

    private final AppreciationId appreciationId;
    private final MemberId authorIdentifier;
    private final LocalDateTime createdOn;
    private String body;
    private LocalDateTime updatedOn;

    public Comment(AppreciationId id, MemberId authorId, String content,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (id == null || authorId == null || createdAt == null) {
            throw new IllegalArgumentException("댓글의 필수 정보가 누락되었습니다.");
        }
        this.appreciationId = id;
        this.authorIdentifier = authorId;
        this.body = requireContent(content);
        this.createdOn = createdAt;
        this.updatedOn = updatedAt;
    }

    public void update(String changedContent, Clock clock) {
        body = requireContent(changedContent);
        updatedOn = LocalDateTime.now(clock);
    }

    public boolean isWrittenBy(MemberId memberId) {
        return authorIdentifier.equals(memberId);
    }

    public String content() {
        return body;
    }

    @Override
    public AppreciationId id() {
        return appreciationId;
    }

    @Override
    public MemberId authorId() {
        return authorIdentifier;
    }

    @Override
    public LocalDateTime createdAt() {
        return createdOn;
    }

    @Override
    public LocalDateTime updatedAt() {
        return updatedOn;
    }

    @Override
    public String kind() {
        return COMMENT_KIND;
    }

    private String requireContent(String value) {
        if (value == null) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        return value;
    }
}
