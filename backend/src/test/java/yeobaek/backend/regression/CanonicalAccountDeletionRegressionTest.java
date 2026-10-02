package yeobaek.backend.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.application.account.AccountDeletionWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
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
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.comment.domain.CommentReport;
import yeobaek.backend.comment.domain.CommentView;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.reading.api.PublicRoomVisitApi;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.support.IntegrationTest;

class CanonicalAccountDeletionRegressionTest extends IntegrationTest {

    @Autowired
    private AccountDeletionWorkflow accountDeletionWorkflow;

    @Autowired
    private CommentSharingWorkflow sharingWorkflow;

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
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository blockRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentViewRepository viewRepository;

    @Autowired
    private CommentReportRepository reportRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Autowired
    private ReadingProgressApi progressApi;

    @Autowired
    private PublicRoomVisitApi visitApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("계정 삭제는 canonical 소유 데이터와 양방향 관계를 지우고 무관한 데이터를 보존한다")
    void eraseEveryCanonicalMemberRelation() {
        Book book = bookRepository.save(newBook(new BookTitle("탈퇴 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, List.of(new SentenceContent("본문"))));
        Club joinedClub = clubRepository.save(newClub(new ClubName("참여 모임"), new JoinCode("DEL001")));
        Club leftClub = clubRepository.save(newClub(new ClubName("탈퇴 모임"), new JoinCode("DEL002")));
        PublicRoom publicRoom = publicRoomRepository.save(newPublicRoom());
        bind(book, joinedClub, leftClub, publicRoom);

        Member target = memberRepository.save(new Member(new Nickname("삭제 대상")));
        Member remaining = memberRepository.save(new Member(new Nickname("잔여 회원")));
        Member observer = memberRepository.save(new Member(new Nickname("관찰 회원")));
        clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(target.getId()), joinedClub));
        ClubMember leftMembership = new ClubMember(new yeobaek.backend.foundation.identity.MemberId(target.getId()), leftClub);
        leftMembership.leave();
        clubMemberRepository.save(leftMembership);
        clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(remaining.getId()), joinedClub));

        var targetClubComment = share(target, joinedClub.getSpaceId(), book, passage, "모임 삭제 댓글");
        var targetRoomComment = share(target, publicRoom.getSpaceId(), book, passage, "공개방 삭제 댓글");
        var remainingComment = share(remaining, joinedClub.getSpaceId(), book, passage, "남을 댓글");
        var targetClubEntity = commentRepository.findById(targetClubComment.value()).orElseThrow();
        var remainingEntity = commentRepository.findById(remainingComment.value()).orElseThrow();
        viewRepository.saveAll(List.of(
                new CommentView(remaining.getId(), targetClubEntity.getId()),
                new CommentView(target.getId(), remainingEntity.getId()),
                new CommentView(observer.getId(), remainingEntity.getId())));
        reportRepository.saveAll(List.of(
                new CommentReport(remaining.getId(), targetClubEntity.getId()),
                new CommentReport(target.getId(), remainingEntity.getId()),
                new CommentReport(observer.getId(), remainingEntity.getId())));

        updateProgress(target, joinedClub.getSpaceId(), book, passage, 1);
        updateProgress(target, leftClub.getSpaceId(), book, passage, 2);
        updateProgress(target, publicRoom.getSpaceId(), book, passage, 3);
        updateProgress(remaining, joinedClub.getSpaceId(), book, passage, 4);
        visitApi.visit(new MemberId(target.getId()), new SpaceId(publicRoom.getSpaceId()),
                LocalDateTime.of(2026, 10, 2, 12, 0));
        visitApi.visit(new MemberId(remaining.getId()), new SpaceId(publicRoom.getSpaceId()),
                LocalDateTime.of(2026, 10, 2, 12, 1));
        blockRepository.saveAll(List.of(
                new MemberBlock(target, remaining),
                new MemberBlock(remaining, target),
                new MemberBlock(observer, remaining)));

        accountDeletionWorkflow.delete(new MemberId(target.getId()));

        assertThat(databaseCounts()).containsExactly(2L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 1L);
        assertThat(List.of(
                commentRepository.existsById(targetClubComment.value()),
                commentRepository.existsById(targetRoomComment.value()),
                commentRepository.existsById(remainingComment.value()),
                memberRepository.existsById(target.getId()),
                memberRepository.existsById(remaining.getId()),
                memberRepository.existsById(observer.getId())))
                .containsExactly(false, false, true, false, true, true);
    }

    private void bind(Book book, Club joinedClub, Club leftClub, PublicRoom publicRoom) {
        ContentId contentId = new ContentId(book.getContentId());
        bindingApi.bind(new SpaceId(joinedClub.getSpaceId()), contentId);
        bindingApi.bind(new SpaceId(leftClub.getSpaceId()), contentId);
        bindingApi.bind(new SpaceId(publicRoom.getSpaceId()), contentId);
    }

    private AppreciationId share(Member author, Long spaceId, Book book, Passage passage, String content) {
        return sharingWorkflow.share(new MemberId(author.getId()), new SpaceId(spaceId),
                new ContentId(book.getContentId()),
                new ContentLocationId(passage.getSentences().getFirst().getLocationId()), content)
                .comment().id();
    }

    private void updateProgress(Member actor, Long spaceId, Book book, Passage passage, int minute) {
        progressApi.update(new MemberId(actor.getId()), new SpaceId(spaceId),
                new ContentId(book.getContentId()),
                new ContentLocationId(passage.getLocationId()),
                LocalDateTime.of(2026, 10, 2, 11, minute));
    }

    private List<Long> databaseCounts() {
        return List.of(
                count("members"),
                count("comments"),
                count("appreciations"),
                count("appreciation_contexts"),
                count("appreciation_views"),
                count("appreciation_reports"),
                count("club_members"),
                count("reading_progresses"),
                count("public_room_visits"),
                count("member_blocks"));
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
