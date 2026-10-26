-- local 프로파일 전용 FCM 테스트 회원과 댓글이다. 적용된 V1.1은 수정하지 않는다.
INSERT INTO members (id, nickname)
VALUES (3, '회원3'), (4, '회원4');

INSERT INTO club_members (id, member_id, club_id, status)
VALUES (3, 3, 1, 'JOINED'), (4, 4, 1, 'JOINED');

INSERT INTO club_member_last_reads (id, club_member_id, last_read_passage_id, last_read_at)
VALUES (3, 3, 2, '2026-08-05 15:00:00'), (4, 4, 2, '2026-08-05 15:00:00');

INSERT INTO comments (id, writer_id, sentence_id, content, created_at, updated_at)
VALUES (3, 3, 2, '회원3도 같은 문장의 댓글 알림을 받을 준비가 됐어요.', CURRENT_TIMESTAMP, NULL),
       (4, 4, 2, '회원4도 같은 문장의 댓글 알림을 받을 준비가 됐어요.', CURRENT_TIMESTAMP, NULL);

INSERT INTO club_comments (id, comment_id, club_id)
VALUES (3, 3, 1), (4, 4, 1);
