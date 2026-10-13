INSERT INTO books (id, title, publisher, published_year, cover_image_key, passage_count, status)
VALUES (10, '이관 도서', '여백', 2026, 'covers/10.webp', 2, 'ACTIVE');
INSERT INTO chapters (id, book_id, title, `sequence`) VALUES (20, 10, '첫 장', 1);
INSERT INTO passages (id, chapter_id, `sequence`) VALUES (30, 20, 1), (31, 20, 2);
INSERT INTO sentences (id, passage_id, `sequence`, content)
VALUES (32, 30, 1, '첫 문장'), (33, 31, 1, '둘째 문장');

INSERT INTO members (id, nickname)
VALUES (40, '모임회원'), (41, '공개회원'), (42, '방문회원'), (43, '탈퇴회원');
INSERT INTO member_blocks (id, blocker_id, blocked_id) VALUES (120, 40, 41);

INSERT INTO clubs (id, book_id, name, join_code) VALUES (60, 10, '이관 모임', 'MIGR01');
INSERT INTO club_members
    (id, member_id, club_id, last_read_passage_id, last_read_at, status)
VALUES
    (70, 40, 60, 30, '2026-10-12 01:02:03.123456', 'JOINED'),
    (71, 41, 60, 31, NULL, 'JOINED'),
    (72, 43, 60, NULL, '2026-10-12 02:03:04.234567', 'LEFT'),
    (73, 42, 60, NULL, NULL, 'JOINED');

INSERT INTO public_rooms (id, book_id) VALUES (50, 10);
INSERT INTO public_room_activities
    (id, member_id, public_room_id, last_visited_at, last_read_passage_id, last_read_at)
VALUES
    (80, 42, 50, '2026-10-12 03:04:05.345678', NULL, NULL),
    (81, 41, 50, NULL, 31, '2026-10-12 04:05:06.456789'),
    (82, 43, 50, NULL, NULL, '2026-10-12 05:06:07.567890'),
    (83, 40, 50, NULL, 30, NULL);

INSERT INTO comments
    (id, club_member_id, public_room_id, writer_id, sentence_id, content, created_at, updated_at)
VALUES
    (90, 70, NULL, NULL, 32, '모임 댓글',
     '2026-10-12 06:07:08.678901', '2026-10-12 07:08:09.789012'),
    (91, NULL, 50, 41, 33, '공개 댓글',
     '2026-10-12 08:09:10.890123', NULL),
    (92, 72, NULL, NULL, 33, '탈퇴 회원 댓글',
     '2026-10-12 09:10:11.901234', NULL);
INSERT INTO comment_reports (id, reporter_id, comment_id) VALUES (100, 40, 91);
INSERT INTO comment_views (id, member_id, comment_id) VALUES (110, 42, 91);
