-- 공개방 코드 배포 전에 운영 MySQL에 한 번 실행한다.
-- 기존 댓글의 ID와 모임 소속은 유지하면서 같은 comments 테이블에 공개방 댓글을 수용한다.

CREATE TABLE IF NOT EXISTS public_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_rooms_book UNIQUE (book_id),
    CONSTRAINT fk_public_rooms_book FOREIGN KEY (book_id) REFERENCES books (id)
);

CREATE TABLE IF NOT EXISTS public_room_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    public_room_id BIGINT NOT NULL,
    last_visited_at DATETIME(6) NULL,
    last_read_passage_id BIGINT NULL,
    last_read_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_room_activities_member_room UNIQUE (member_id, public_room_id),
    CONSTRAINT fk_public_room_activities_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_public_room_activities_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id),
    CONSTRAINT fk_public_room_activities_passage FOREIGN KEY (last_read_passage_id) REFERENCES passages (id)
);

ALTER TABLE comments MODIFY club_member_id BIGINT NULL;
ALTER TABLE comments ADD COLUMN public_room_id BIGINT NULL;
ALTER TABLE comments ADD COLUMN writer_id BIGINT NULL;
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_public_room FOREIGN KEY (public_room_id) REFERENCES public_rooms (id),
    ADD CONSTRAINT fk_comments_writer FOREIGN KEY (writer_id) REFERENCES members (id),
    ADD CONSTRAINT ck_comments_single_space CHECK (
        (club_member_id IS NOT NULL AND public_room_id IS NULL AND writer_id IS NULL)
        OR
        (club_member_id IS NULL AND public_room_id IS NOT NULL AND writer_id IS NOT NULL)
    );

INSERT INTO public_rooms (book_id)
SELECT id FROM books
ON DUPLICATE KEY UPDATE book_id = VALUES(book_id);
