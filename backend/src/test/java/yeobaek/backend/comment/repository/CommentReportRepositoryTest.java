package yeobaek.backend.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.CommentReport;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class CommentReportRepositoryTest extends IntegrationTest {

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Test
    @DisplayName("같은 신고자와 댓글의 신고는 중복 저장할 수 없다")
    void rejectDuplicateReport() {
        ReportFixture fixture = createReportFixture("DUP001");
        commentReportRepository.saveAndFlush(new CommentReport(fixture.reporter(), fixture.comment()));

        assertThatThrownBy(() -> commentReportRepository.saveAndFlush(
                new CommentReport(fixture.reporter(), fixture.comment())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("신고자 계정이 삭제되면 DB cascade로 신고가 삭제된다")
    void deleteByReporterCascade() {
        ReportFixture fixture = createReportFixture("CAS001");
        commentReportRepository.saveAndFlush(new CommentReport(fixture.reporter(), fixture.comment()));

        memberRepository.deleteById(fixture.reporter().getId());
        memberRepository.flush();

        assertThat(commentReportRepository.count()).isZero();
    }

    @Test
    @DisplayName("대상 댓글이 삭제되면 DB cascade로 신고가 삭제된다")
    void deleteByCommentCascade() {
        ReportFixture fixture = createReportFixture("CAS002");
        commentReportRepository.saveAndFlush(new CommentReport(fixture.reporter(), fixture.comment()));

        commentRepository.deleteById(fixture.comment().getId());
        commentRepository.flush();

        assertThat(commentReportRepository.count()).isZero();
    }

    private ReportFixture createReportFixture(String joinCode) {
        Book book = bookRepository.save(new Book("신고 테스트 도서", null, 1924, 1));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, "본문"));
        Member writer = memberRepository.save(new Member("작성자"));
        Member reporter = memberRepository.save(new Member("신고자"));
        Club club = clubRepository.save(new Club("신고 모임", book, joinCode));
        ClubMember writerMembership = clubMemberRepository.save(new ClubMember(writer, club));
        Comment comment = commentRepository.saveAndFlush(
                new Comment(writerMembership, passage.getSentences().getFirst(), "신고 대상"));
        return new ReportFixture(reporter, comment);
    }

    private record ReportFixture(Member reporter, Comment comment) {
    }
}
