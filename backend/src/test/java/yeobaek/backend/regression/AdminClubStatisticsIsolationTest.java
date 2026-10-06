package yeobaek.backend.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.service.AdminClubDashboardService;
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
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.web.v1.CommentService;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.space.publicroom.persistence.PublicRoom;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class AdminClubStatisticsIsolationTest extends IntegrationTest {

    @Autowired
    private AdminClubDashboardService dashboardService;

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
    private CommentRepository commentRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Test
    @DisplayName("관리자 모임 통계는 같은 도서의 공개방 댓글을 모임 댓글 수에 포함하지 않는다")
    void countOnlyClubCommentsWhenPublicRoomUsesSameBook() {
        Book book = bookRepository.save(newBook(new BookTitle("통계 공유 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, List.of(new SentenceContent("본문"))));
        Club club = clubRepository.save(newClub(new ClubName("통계 모임"), new JoinCode("STAT01")));
        PublicRoom room = publicRoomRepository.save(newPublicRoom());
        ContentId contentId = new ContentId(book.getContentId());
        bindingApi.bind(new SpaceId(club.getSpaceId()), contentId);
        bindingApi.bind(new SpaceId(room.getSpaceId()), contentId);
        Member writer = memberRepository.save(new Member(new Nickname("통계 작성자")));
        clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(writer.getId()), club));
        Long sentenceId = passage.getSentences().getFirst().getId();
        commentService.create(writer.getId(), club.getId(), sentenceId, new CommentContent("모임 댓글"));
        commentService.createInPublicRoom(writer.getId(), room.getId(), sentenceId, new CommentContent("공개방 댓글 1"));
        commentService.createInPublicRoom(writer.getId(), room.getId(), sentenceId, new CommentContent("공개방 댓글 2"));

        var response = dashboardService.findClubsWithMemberAndCommentCounts();

        assertThat(response.clubs())
                .extracting(AdminDashboardClubResponse::clubId,
                        AdminDashboardClubResponse::memberCount,
                        AdminDashboardClubResponse::commentCount)
                .containsExactly(tuple(club.getId(), 1L, 1L));
        assertThat(commentRepository.count()).isEqualTo(3L);
    }
}
