package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
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
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.IntegrationTest;

class AdminClubDashboardServiceTest extends IntegrationTest {

    @Autowired
    private AdminClubDashboardService adminClubDashboardService;

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
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private MemberService memberService;

    @Test
    @DisplayName("모임별 참여자와 댓글을 중복 없이 집계하고 탈퇴자 댓글과 삭제 도서를 보존한다")
    void findClubsWithCounts() {
        Book book = bookRepository.save(new Book(new BookTitle("통계 도서"), null, null, 1, null));
        Passage passage = createPassage(book);
        Club first = clubRepository.save(new Club(new ClubName("첫 모임"), book, new JoinCode("DASH01")));
        Club empty = clubRepository.save(new Club(new ClubName("빈 모임"), book, new JoinCode("DASH02")));
        Member joined = memberRepository.save(new Member(new Nickname("참여 회원")));
        Member left = memberRepository.save(new Member(new Nickname("탈퇴 회원")));
        Member deleted = memberRepository.save(new Member(new Nickname("삭제 회원")));
        ClubMember joinedMembership = clubMemberRepository.save(new ClubMember(joined, first));
        ClubMember leftMembership = new ClubMember(left, first);
        leftMembership.leave();
        leftMembership = clubMemberRepository.save(leftMembership);
        ClubMember deletedMembership = clubMemberRepository.save(new ClubMember(deleted, first));
        memberBlockRepository.save(new MemberBlock(joined, left));
        commentRepository.saveAll(List.of(
                new Comment(joinedMembership, passage.getSentences().getFirst(), new CommentContent("첫 댓글")),
                new Comment(joinedMembership, passage.getSentences().getFirst(), new CommentContent("둘째 댓글")),
                new Comment(leftMembership, passage.getSentences().getFirst(), new CommentContent("탈퇴자 댓글")),
                new Comment(deletedMembership, passage.getSentences().getFirst(), new CommentContent("삭제될 댓글"))));
        memberService.delete(deleted.getId());
        bookRepository.delete(book.getId());

        AdminDashboardClubsResponse response = adminClubDashboardService.findClubs();

        assertThat(response.clubs())
                .extracting(
                        AdminDashboardClubResponse::clubId,
                        AdminDashboardClubResponse::name,
                        AdminDashboardClubResponse::bookStatus,
                        AdminDashboardClubResponse::memberCount,
                        AdminDashboardClubResponse::commentCount)
                .containsExactly(
                        tuple(first.getId(), "첫 모임", BookStatus.DELETED, 1L, 3L),
                        tuple(empty.getId(), "빈 모임", BookStatus.DELETED, 0L, 0L));
        assertThat(response.clubs().getFirst())
                .extracting(AdminDashboardClubResponse::bookId, AdminDashboardClubResponse::bookTitle)
                .containsExactly(book.getId(), "통계 도서");
    }

    @Test
    @DisplayName("모임이 없으면 빈 목록을 반환한다")
    void findClubsWhenEmpty() {
        assertThat(adminClubDashboardService.findClubs().clubs()).isEmpty();
    }

    private Passage createPassage(Book book) {
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        return passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("첫 문장"))));
    }
}
