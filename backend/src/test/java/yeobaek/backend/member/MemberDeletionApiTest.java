package yeobaek.backend.member;

import yeobaek.backend.support.CommentFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.book.domain.Book;
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
import yeobaek.backend.comment.domain.CommentReport;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentReportRepository;
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
import yeobaek.backend.reading.persistence.ReadingProgressJpaEntity;
import yeobaek.backend.reading.persistence.ReadingProgressRepository;
import yeobaek.backend.support.IntegrationTest;

class MemberDeletionApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

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
    private CommentReportRepository commentReportRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    @Autowired
    private AppreciationContextRepository appreciationContextRepository;

    @Test
    @DisplayName("계정 삭제는 대상 회원의 댓글과 모든 참여·진도를 삭제하고 다른 데이터는 보존한다")
    void deleteMemberData() throws Exception {
        Book book = bookRepository.save(newBook(new BookTitle("회원 탈퇴 테스트 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, List.of(new SentenceContent("첫 문장."))));
        Member targetMember = memberRepository.save(new Member(new Nickname("탈퇴 회원")));
        Member remainingMember = memberRepository.save(new Member(new Nickname("잔여 회원")));
        Club sharedClub = clubRepository.save(newClub(new ClubName("공유 모임"), new JoinCode("DELETE")));
        Club leftClub = clubRepository.save(newClub(new ClubName("탈퇴한 모임"), new JoinCode("LEFT01")));
        ContentId contentId = new ContentId(book.getContentId());
        bindingApi.bind(new SpaceId(sharedClub.getSpaceId()), contentId);
        bindingApi.bind(new SpaceId(leftClub.getSpaceId()), contentId);

        ClubMember progressedMembership = new ClubMember(new yeobaek.backend.foundation.identity.MemberId(targetMember.getId()), sharedClub);
        progressedMembership = clubMemberRepository.save(progressedMembership);
        readingProgressRepository.save(new ReadingProgressJpaEntity(targetMember.getId(),
                sharedClub.getSpaceId(), contentId.value(), passage.getLocationId(),
                LocalDateTime.of(2026, 9, 3, 10, 0)));
        ClubMember leftMembership = new ClubMember(new yeobaek.backend.foundation.identity.MemberId(targetMember.getId()), leftClub);
        leftMembership.leave();
        leftMembership = clubMemberRepository.save(leftMembership);
        ClubMember remainingMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(remainingMember.getId()), sharedClub));
        memberBlockRepository.saveAll(List.of(
                new MemberBlock(targetMember, remainingMember),
                new MemberBlock(remainingMember, targetMember)));

        Comment targetComment = commentRepository.save(
                newClubComment(progressedMembership, passage.getSentences().getFirst(), new CommentContent("삭제될 댓글")));
        Comment leftTargetComment = commentRepository.save(
                newClubComment(leftMembership, passage.getSentences().getFirst(), new CommentContent("탈퇴 모임의 삭제될 댓글")));
        Comment remainingComment = commentRepository.save(
                newClubComment(remainingMembership, passage.getSentences().getFirst(), new CommentContent("남을 댓글")));
        appreciationContextRepository.saveAll(List.of(
                CommentFixtures.contextOf(targetComment),
                CommentFixtures.contextOf(leftTargetComment),
                CommentFixtures.contextOf(remainingComment)));
        commentReportRepository.saveAllAndFlush(List.of(
                new CommentReport(remainingMember.getId(), targetComment.getId()),
                new CommentReport(targetMember.getId(), remainingComment.getId())));

        mockMvc.perform(delete("/api/members/me")
                        .header("X-Member-Id", targetMember.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/api/members/me")
                        .header("X-Member-Id", targetMember.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));

        assertThat(memberRepository.findAll())
                .extracting(Member::getId)
                .containsExactly(remainingMember.getId());
        assertThat(commentRepository.findAll())
                .extracting(Comment::getId)
                .containsExactly(remainingComment.getId())
                .doesNotContain(targetComment.getId(), leftTargetComment.getId());
        assertThat(clubMemberRepository.findAll())
                .extracting(ClubMember::getId)
                .containsExactly(remainingMembership.getId())
                .doesNotContain(progressedMembership.getId(), leftMembership.getId());
        assertThat(List.of(memberBlockRepository.count(), commentReportRepository.count())).containsOnly(0L);
        assertThat(clubRepository.findAll())
                .extracting(Club::getId)
                .containsExactlyInAnyOrder(sharedClub.getId(), leftClub.getId());
    }
}
