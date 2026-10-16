"""Deterministic rows for the reading schema; no development data is read."""

from itertools import islice


TABLES = (
    "authors", "books", "author_books", "chapters", "passages", "sentences",
    "members", "clubs", "club_books", "club_members", "club_member_last_reads",
    "public_rooms", "public_room_members", "public_room_member_last_reads",
    "comments", "club_comments", "public_room_comments",
    "member_blocks", "comment_views", "reports",
)
STAMP = "2026-01-01 00:00:00"


def comments_per_book(sentence_count):
    blocks, remainder = divmod(sentence_count, 10)
    return blocks * 3 + sum(position <= remainder for position in (1, 4, 7))


def join_code(number):
    digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    result = ""
    for _ in range(6):
        number, remainder = divmod(number, 36)
        result = digits[remainder] + result
    if number:
        raise ValueError("Club count exceeds the six-character join-code capacity")
    return result


def validate(args):
    if args.members_per_club > args.members:
        raise ValueError("members-per-club must not exceed members")
    join_code(args.clubs - 1)
    if not args.sentence_text.strip() or len(args.sentence_text.encode("utf-8")) > 65535:
        raise ValueError("sentence-text must be nonblank and fit MySQL TEXT (65,535 UTF-8 bytes)")
    if not args.comment_text.strip() or len(args.comment_text.encode("utf-16-le")) // 2 > 1000:
        raise ValueError("comment-text must be nonblank and fit the Java 1000-character limit")


def dimensions(args):
    passages = args.chapters_per_book * args.passages_per_chapter
    return passages, passages * args.sentences_per_passage


def membership(args, group_id, slot):
    return ((group_id - 1) * args.members_per_club + slot) % args.members + 1


def comment_rows(args):
    """One comment per selected sentence, split between club/public spaces."""
    _, sentence_count = dimensions(args)
    per_book = comments_per_book(sentence_count)
    for book_id in range(1, args.books + 1):
        book_clubs = range(book_id, args.clubs + 1, args.books)
        ordinal = 0
        for block in range((sentence_count + 9) // 10):
            for position in (1, 4, 7):
                local_sentence = block * 10 + position
                if local_sentence > sentence_count:
                    continue
                comment_id = (book_id - 1) * per_book + ordinal + 1
                sentence_id = (book_id - 1) * sentence_count + local_sentence
                slot = ordinal % args.members_per_club
                if book_clubs and ordinal % 2 == 0:
                    club_id = book_clubs[(ordinal // 2) % len(book_clubs)]
                    writer = membership(args, club_id, slot)
                    space, space_id = "club", club_id
                else:
                    writer = membership(args, book_id, slot)
                    space, space_id = "public", book_id
                yield comment_id, writer, sentence_id, space, space_id
                ordinal += 1


def plan(args):
    validate(args)
    passage_count, sentence_count = dimensions(args)
    per_book = comments_per_book(sentence_count)
    books_with_club = min(args.books, args.clubs)
    club_comments = books_with_club * ((per_book + 1) // 2)
    total_comments = args.books * per_book
    return {
        "authors": 1, "books": args.books, "author_books": args.books,
        "chapters": args.books * args.chapters_per_book,
        "passages": args.books * passage_count,
        "sentences": args.books * sentence_count,
        "members": args.members, "clubs": args.clubs, "club_books": args.clubs,
        "club_members": args.clubs * args.members_per_club,
        "club_member_last_reads": args.clubs * args.members_per_club,
        "public_rooms": args.books,
        "public_room_members": args.books * args.members_per_club,
        "public_room_member_last_reads": args.books * args.members_per_club,
        "comments": total_comments, "club_comments": club_comments,
        "public_room_comments": total_comments - club_comments,
        "member_blocks": 0, "comment_views": 0, "reports": 0,
    }


def rows(args):
    """Yield table, columns and row iterators in foreign-key dependency order."""
    passage_count, sentence_count = dimensions(args)
    middle = max(1, passage_count // 2)
    yield "authors", "id,name,isni", [(1, "부하테스트 저자", None)]
    yield "books", "id,title,publisher,published_year,cover_image_key,passage_count,status", (
        (b, f"부하테스트 책 {b}", "부하테스트", None, None, passage_count, "ACTIVE")
        for b in range(1, args.books + 1)
    )
    yield "author_books", "id,author_id,book_id", ((b, 1, b) for b in range(1, args.books + 1))
    yield "chapters", "id,book_id,title,`sequence`", (
        ((b - 1) * args.chapters_per_book + ch, b, f"부하테스트 장 {ch}", ch)
        for b in range(1, args.books + 1) for ch in range(1, args.chapters_per_book + 1)
    )
    yield "passages", "id,chapter_id,`sequence`", (
        ((b - 1) * passage_count + p,
         (b - 1) * args.chapters_per_book + (p - 1) // args.passages_per_chapter + 1, p)
        for b in range(1, args.books + 1) for p in range(1, passage_count + 1)
    )
    yield "sentences", "id,passage_id,`sequence`,content", (
        ((b - 1) * sentence_count + s,
         (b - 1) * passage_count + (s - 1) // args.sentences_per_passage + 1,
         (s - 1) % args.sentences_per_passage + 1, args.sentence_text)
        for b in range(1, args.books + 1) for s in range(1, sentence_count + 1)
    )
    yield "members", "id,nickname", ((m, f"LT{m:x}") for m in range(1, args.members + 1))
    yield "clubs", "id,name,join_code", ((c, f"LT{c:x}", join_code(c - 1)) for c in range(1, args.clubs + 1))
    yield "club_books", "id,club_id,book_id", ((c, c, (c - 1) % args.books + 1) for c in range(1, args.clubs + 1))
    yield "club_members", "id,member_id,club_id,status", (
        ((c - 1) * args.members_per_club + slot + 1, membership(args, c, slot), c, "JOINED")
        for c in range(1, args.clubs + 1) for slot in range(args.members_per_club)
    )
    yield "club_member_last_reads", "id,club_member_id,last_read_passage_id,last_read_at", (
        ((c - 1) * args.members_per_club + slot + 1,
         (c - 1) * args.members_per_club + slot + 1,
         ((c - 1) % args.books) * passage_count + middle, STAMP)
        for c in range(1, args.clubs + 1) for slot in range(args.members_per_club)
    )
    yield "public_rooms", "id,book_id", ((b, b) for b in range(1, args.books + 1))
    yield "public_room_members", "id,member_id,public_room_id,last_visited_at", (
        ((b - 1) * args.members_per_club + slot + 1, membership(args, b, slot), b, STAMP)
        for b in range(1, args.books + 1) for slot in range(args.members_per_club)
    )
    yield "public_room_member_last_reads", "id,public_room_member_id,last_read_passage_id,last_read_at", (
        ((b - 1) * args.members_per_club + slot + 1,
         (b - 1) * args.members_per_club + slot + 1, (b - 1) * passage_count + middle, STAMP)
        for b in range(1, args.books + 1) for slot in range(args.members_per_club)
    )
    yield "comments", "id,writer_id,sentence_id,content,created_at,updated_at", (
        (cid, writer, sid, args.comment_text, "2026-01-01 00:00:01", None)
        for cid, writer, sid, _, _ in comment_rows(args)
    )
    yield "club_comments", "id,comment_id,club_id", (
        (cid, cid, space_id) for cid, _, _, space, space_id in comment_rows(args) if space == "club"
    )
    yield "public_room_comments", "id,comment_id,public_room_id", (
        (cid, cid, space_id) for cid, _, _, space, space_id in comment_rows(args) if space == "public"
    )


def literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, int):
        return str(value)
    return "CONVERT(0x" + value.encode("utf-8").hex() + " USING utf8mb4)"


def clear_rows(db):
    # Child rows go first. Keep foreign-key checks enabled and do not run DDL.
    for table in reversed(TABLES):
        db.query(f"DELETE FROM `{table}`")
    for table in TABLES:
        if int(db.query(f"SELECT EXISTS(SELECT 1 FROM `{table}` LIMIT 1)")[0][0]):
            raise RuntimeError("Reset did not empty " + table)


def seed(db, args):
    expected = plan(args)
    db.query("START TRANSACTION")
    clear_rows(db)
    for table, columns, iterator in rows(args):
        iterator = iter(iterator)
        while True:
            batch = list(islice(iterator, args.batch_size))
            if not batch:
                break
            values = ",".join("(" + ",".join(literal(value) for value in row) + ")" for row in batch)
            db.query(f"INSERT INTO `{table}` ({columns}) VALUES {values}")
        actual = int(db.query(f"SELECT COUNT(*) FROM `{table}`")[0][0])
        if actual != expected[table]:
            raise RuntimeError(f"Unexpected row count in {table}: {actual} != {expected[table]}")
    db.query("COMMIT")
    return expected
