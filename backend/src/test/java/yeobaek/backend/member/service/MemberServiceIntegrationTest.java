package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.publicroom.service.PublicRoomService;
import yeobaek.backend.support.IntegrationTest;

class MemberServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PublicRoomService publicRoomService;

    @Autowired
    private PublicRoomMemberRepository publicRoomMemberRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("계정 삭제는 공개방 활동과 댓글을 제거해 인기 집계에서도 제외한다")
    void memberDeletion() {
        ReadingFixture fixture = createRoom("탈퇴 책", 1);
        Member leaving = memberRepository.save(new Member(new Nickname("탈퇴자")));
        Member remaining = memberRepository.save(new Member(new Nickname("잔여자")));
        publicRoomService.visit(leaving.getId(), fixture.room().getId());
        publicRoomService.updateProgress(leaving.getId(), fixture.room().getId(),
                fixture.passages().getFirst().getId());
        publicRoomService.visit(remaining.getId(), fixture.room().getId());
        publicRoomService.updateProgress(remaining.getId(), fixture.room().getId(),
                fixture.passages().getFirst().getId());
        Long sentenceId = fixture.passages().getFirst().getSentences().getFirst().getId();
        commentService.createInPublicRoom(leaving.getId(), fixture.room().getId(), sentenceId,
                new CommentContent("삭제될 댓글"));

        memberService.delete(leaving.getId());

        assertThat(publicRoomMemberRepository.findAll()).hasSize(1)
                .allMatch(membership -> membership.getMember().getId().equals(remaining.getId()));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from public_room_member_last_reads", Long.class)).isOne();
        assertThat(commentRepository.findAll()).isEmpty();
        assertThat(publicRoomRepository.findAll()).hasSize(1);
    }

    private ReadingFixture createRoom(String title, int passageCount) {
        Book book = bookRepository.save(new Book(new BookTitle(title), null, null, passageCount, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        List<Passage> passages = java.util.stream.IntStream.rangeClosed(1, passageCount)
                .mapToObj(sequence -> passageRepository.save(new Passage(chapter, sequence,
                        List.of(new SentenceContent(sequence + "번 문장")))))
                .toList();
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        return new ReadingFixture(book, room, passages);
    }

    private record ReadingFixture(Book book, PublicRoom room, List<Passage> passages) {
    }
}
