package yeobaek.backend.appreciation.comment.repository;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.application.account.AccountDeletionWorkflow;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.domain.Chapter;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.space.club.persistence.Club;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.comment.domain.CommentReport;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.shared.identity.MemberId;
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

    @Autowired
    private CommentApi commentApi;

    @Autowired
    private AccountDeletionWorkflow accountDeletionWorkflow;

    @Test
    @DisplayName("같은 신고자와 댓글의 신고는 중복 저장할 수 없다")
    void rejectDuplicateReport() {
        ReportFixture fixture = createReportFixture("DUP001");
        commentReportRepository.saveAndFlush(new CommentReport(
                fixture.reporter().getId(), fixture.comment().getId()));

        assertThatThrownBy(() -> commentReportRepository.saveAndFlush(
                new CommentReport(fixture.reporter().getId(), fixture.comment().getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("신고자 계정이 삭제되면 DB cascade로 신고가 삭제된다")
    void deleteByReporterCascade() {
        ReportFixture fixture = createReportFixture("CAS001");
        commentReportRepository.saveAndFlush(new CommentReport(
                fixture.reporter().getId(), fixture.comment().getId()));

        accountDeletionWorkflow.delete(new MemberId(fixture.reporter().getId()));

        assertThat(commentReportRepository.count()).isZero();
    }

    @Test
    @DisplayName("대상 댓글이 삭제되면 DB cascade로 신고가 삭제된다")
    void deleteByCommentCascade() {
        ReportFixture fixture = createReportFixture("CAS002");
        commentReportRepository.saveAndFlush(new CommentReport(
                fixture.reporter().getId(), fixture.comment().getId()));

        commentApi.delete(new MemberId(fixture.writer().getId()),
                new yeobaek.backend.shared.identity.AppreciationId(fixture.comment().getId()));

        assertThat(commentReportRepository.count()).isZero();
    }

    private ReportFixture createReportFixture(String joinCode) {
        Book book = bookRepository.save(newBook(new BookTitle("신고 테스트 도서"), null, 1924, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, Collections.singletonList(new SentenceContent("본문"))));
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));
        Member reporter = memberRepository.save(new Member(new Nickname("신고자")));
        Club club = clubRepository.save(newClub(new ClubName("신고 모임"), new JoinCode(joinCode)));
        ClubMember writerMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(writer.getId()), club));
        Comment comment = commentRepository.saveAndFlush(
                newClubComment(writerMembership, passage.getSentences().getFirst(), new CommentContent("신고 대상")));
        return new ReportFixture(writer, reporter, comment);
    }

    private record ReportFixture(Member writer, Member reporter, Comment comment) {
    }
}
