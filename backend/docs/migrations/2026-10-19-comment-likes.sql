-- 독서 스키마 전환 완료 후, 좋아요 엔티티를 포함하는 서버 배포 전에 적용한다.
CREATE TABLE comment_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_comment_likes_comment_member UNIQUE (comment_id, member_id),
    CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_likes_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
);
