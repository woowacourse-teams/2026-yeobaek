package yeobaek.backend.admin.service;

import yeobaek.backend.support.CommentFixtures;


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
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.web.compatibility.MemberService;
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
    private AppreciationContextRepository appreciationContextRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Test
    @DisplayName("모임별 참여자와 댓글을 중복 없이 집계하고 탈퇴자 댓글과 삭제 도서를 보존한다")
    void findClubsWithMemberAndCommentCounts() {
        Book book = bookRepository.save(newBook(new BookTitle("통계 도서"), null, null, 1, null));
        Passage passage = createPassage(book);
        Club first = clubRepository.save(newClub(new ClubName("첫 모임"), new JoinCode("DASH01")));
        Club empty = clubRepository.save(newClub(new ClubName("빈 모임"), new JoinCode("DASH02")));
        Club second = clubRepository.save(newClub(new ClubName("둘째 모임"), new JoinCode("DASH03")));
        ContentId contentId = new ContentId(book.getContentId());
        bindingApi.bind(new SpaceId(first.getSpaceId()), contentId);
        bindingApi.bind(new SpaceId(empty.getSpaceId()), contentId);
        bindingApi.bind(new SpaceId(second.getSpaceId()), contentId);
        Member joined = memberRepository.save(new Member(new Nickname("참여 회원")));
        Member left = memberRepository.save(new Member(new Nickname("탈퇴 회원")));
        Member deleted = memberRepository.save(new Member(new Nickname("삭제 회원")));
        Member secondJoined = memberRepository.save(new Member(new Nickname("둘째 모임 회원")));
        ClubMember joinedMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(joined.getId()), first));
        ClubMember leftMembership = new ClubMember(new yeobaek.backend.foundation.identity.MemberId(left.getId()), first);
        leftMembership.leave();
        leftMembership = clubMemberRepository.save(leftMembership);
        ClubMember deletedMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(deleted.getId()), first));
        ClubMember secondMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(joined.getId()), second));
        clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(secondJoined.getId()), second));
        memberBlockRepository.save(new MemberBlock(joined, left));
        saveComments(List.of(
                newClubComment(joinedMembership, passage.getSentences().getFirst(), new CommentContent("첫 댓글")),
                newClubComment(joinedMembership, passage.getSentences().getFirst(), new CommentContent("둘째 댓글")),
                newClubComment(leftMembership, passage.getSentences().getFirst(), new CommentContent("탈퇴자 댓글")),
                newClubComment(deletedMembership, passage.getSentences().getFirst(), new CommentContent("삭제될 댓글")),
                newClubComment(secondMembership, passage.getSentences().getFirst(), new CommentContent("다른 모임 댓글"))));
        memberService.delete(deleted.getId());
        bookRepository.delete(book.getId());

        AdminDashboardClubsResponse response = adminClubDashboardService.findClubsWithMemberAndCommentCounts();

        assertThat(response.clubs())
                .extracting(
                        AdminDashboardClubResponse::clubId,
                        AdminDashboardClubResponse::name,
                        AdminDashboardClubResponse::bookStatus,
                        AdminDashboardClubResponse::memberCount,
                        AdminDashboardClubResponse::commentCount)
                .containsExactly(
                        tuple(first.getId(), "첫 모임", BookStatus.DELETED, 1L, 3L),
                        tuple(empty.getId(), "빈 모임", BookStatus.DELETED, 0L, 0L),
                        tuple(second.getId(), "둘째 모임", BookStatus.DELETED, 2L, 1L));
        assertThat(response.clubs().getFirst())
                .extracting(AdminDashboardClubResponse::bookId, AdminDashboardClubResponse::bookTitle)
                .containsExactly(book.getId(), "통계 도서");
    }

    @Test
    @DisplayName("모임이 없으면 빈 목록을 반환한다")
    void findClubsWithMemberAndCommentCountsWhenEmpty() {
        assertThat(adminClubDashboardService.findClubsWithMemberAndCommentCounts().clubs()).isEmpty();
    }

    private Passage createPassage(Book book) {
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        return passageRepository.save(newPassage(chapter, 1, List.of(new SentenceContent("첫 문장"))));
    }

    private void saveComments(List<Comment> comments) {
        commentRepository.saveAll(comments);
        appreciationContextRepository.saveAll(comments.stream().map(CommentFixtures::contextOf).toList());
    }
}
