package yeobaek.backend.member;

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
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
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

    @Test
    @DisplayName("계정 삭제는 대상 회원의 댓글과 모든 참여·진도를 삭제하고 다른 데이터는 보존한다")
    void deleteMemberData() throws Exception {
        Book book = bookRepository.save(new Book("회원 탈퇴 테스트 도서", null, null, 1));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, List.of("첫 문장.")));
        Member targetMember = memberRepository.save(new Member("탈퇴 회원"));
        Member remainingMember = memberRepository.save(new Member("잔여 회원"));
        Club sharedClub = clubRepository.save(new Club("공유 모임", book, "DELETE"));
        Club leftClub = clubRepository.save(new Club("탈퇴한 모임", book, "LEFT01"));

        ClubMember progressedMembership = new ClubMember(targetMember, sharedClub);
        progressedMembership.updateProgress(passage, LocalDateTime.of(2026, 9, 3, 10, 0));
        progressedMembership = clubMemberRepository.save(progressedMembership);
        ClubMember leftMembership = new ClubMember(targetMember, leftClub);
        leftMembership.leave();
        leftMembership = clubMemberRepository.save(leftMembership);
        ClubMember remainingMembership = clubMemberRepository.save(new ClubMember(remainingMember, sharedClub));
        memberBlockRepository.saveAll(List.of(
                new MemberBlock(targetMember, remainingMember),
                new MemberBlock(remainingMember, targetMember)));

        Comment targetComment = commentRepository.save(
                new Comment(progressedMembership, passage.getSentences().getFirst(), "삭제될 댓글"));
        Comment leftTargetComment = commentRepository.save(
                new Comment(leftMembership, passage.getSentences().getFirst(), "탈퇴 모임의 삭제될 댓글"));
        Comment remainingComment = commentRepository.save(
                new Comment(remainingMembership, passage.getSentences().getFirst(), "남을 댓글"));

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
        assertThat(memberBlockRepository.findAll()).isEmpty();
        assertThat(clubRepository.findAll())
                .extracting(Club::getId)
                .containsExactlyInAnyOrder(sharedClub.getId(), leftClub.getId());
    }
}
