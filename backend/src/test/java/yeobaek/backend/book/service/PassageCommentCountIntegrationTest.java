package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.DatabaseCleaningIntegrationTest;

class PassageCommentCountIntegrationTest extends DatabaseCleaningIntegrationTest {

    @Autowired
    private PassageService passageService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    private Member reader;
    private Club club;
    private Club otherClub;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book(new BookTitle("운수 좋은 날"), null, 1924, 5, null, null, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        for (int sequence = 1; sequence <= 5; sequence++) {
            passageRepository.save(new Passage(chapter, sequence, Collections.singletonList(new SentenceContent("본문 " + sequence))));
        }
        reader = memberRepository.save(new Member(new Nickname("민서")));
        club = clubRepository.save(new Club(new ClubName("1기"), book, new JoinCode("CODE01")));
        otherClub = clubRepository.save(new Club(new ClubName("2기"), book, new JoinCode("CODE02")));
    }

    @Test
    @DisplayName("범위의 본문과 모임 내 댓글 수를 함께 조회한다")
    void findPassagesWithCommentCounts() {
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(reader, club));
        ClubMember otherClubMember = clubMemberRepository.save(new ClubMember(reader, otherClub));
        Passage second = passageRepository.findRangeByBookId(club.getBook().getId(), 2, 2).getFirst();
        var sentence = second.getSentences().getFirst();
        commentRepository.save(new Comment(clubMember, sentence, new CommentContent("우리 모임 댓글 1")));
        commentRepository.save(new Comment(clubMember, sentence, new CommentContent("우리 모임 댓글 2")));
        commentRepository.save(new Comment(otherClubMember, sentence, new CommentContent("다른 모임 댓글")));

        PassagesResponse response = passageService.findPassages(reader.getId(), club.getId(), 1, 3);

        assertThat(response.passages()).hasSize(3);
        assertThat(response.passages().get(0).sentences().getFirst().commentCount()).isZero();
        assertThat(response.passages().get(1).sequence()).isEqualTo(2);
        assertThat(response.passages().get(1).sentences().getFirst().commentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("댓글 신고 전후 본문 응답의 댓글 수는 동일하다")
    void preserveCommentCountAfterReport() {
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));
        clubMemberRepository.save(new ClubMember(reader, club));
        ClubMember writerMembership = clubMemberRepository.save(new ClubMember(writer, club));
        Passage first = passageRepository.findRangeByBookId(club.getBook().getId(), 1, 1).getFirst();
        Comment comment = commentRepository.save(
                new Comment(writerMembership, first.getSentences().getFirst(), new CommentContent("신고 대상")));

        PassagesResponse beforeReport = passageService.findPassages(reader.getId(), club.getId(), 1, 1);
        commentService.report(reader.getId(), comment.getId());
        PassagesResponse afterReport = passageService.findPassages(reader.getId(), club.getId(), 1, 1);

        assertThat(beforeReport.passages().getFirst().sentences().getFirst().commentCount()).isEqualTo(1);
        assertThat(afterReport.passages().getFirst().sentences().getFirst().commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("문장 댓글 수에서 요청자가 차단한 회원의 댓글을 제외한다")
    void excludeBlockedWritersFromCommentCounts() {
        Member blockedWriter = memberRepository.save(new Member(new Nickname("차단 대상")));
        ClubMember readerMembership = clubMemberRepository.save(new ClubMember(reader, club));
        ClubMember blockedMembership = clubMemberRepository.save(new ClubMember(blockedWriter, club));
        Passage first = passageRepository.findRangeByBookId(club.getBook().getId(), 1, 1).getFirst();
        var sentence = first.getSentences().getFirst();
        commentRepository.save(new Comment(readerMembership, sentence, new CommentContent("보이는 댓글")));
        commentRepository.save(new Comment(blockedMembership, sentence, new CommentContent("숨길 댓글")));
        memberBlockRepository.save(new MemberBlock(reader, blockedWriter));

        PassagesResponse response = passageService.findPassages(reader.getId(), club.getId(), 1, 1);

        assertThat(response.passages().getFirst().sentences().getFirst().commentCount()).isEqualTo(1);
    }

}
