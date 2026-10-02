package yeobaek.backend.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.web.compatibility.CommentService;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class CrossSpaceCommentIsolationTest extends IntegrationTest {

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
    private SpaceContentBindingApi bindingApi;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentViewRepository viewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Club club;
    private PublicRoom publicRoom;
    private Passage passage;
    private Member writer;
    private Member reader;

    @BeforeEach
    void setUpReadingSpaces() {
        Book book = bookRepository.save(newBook(new BookTitle("공유 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        passage = passageRepository.save(newPassage(chapter, 1, List.of(new SentenceContent("같은 문장"))));
        club = clubRepository.save(newClub(new ClubName("공유 모임"), new JoinCode("CROSS1")));
        publicRoom = publicRoomRepository.save(newPublicRoom());
        bindingApi.bind(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId()));
        bindingApi.bind(new SpaceId(publicRoom.getSpaceId()), new ContentId(book.getContentId()));
        writer = memberRepository.save(new Member(new Nickname("작성자")));
        reader = memberRepository.save(new Member(new Nickname("독자")));
        clubMemberRepository.saveAll(List.of(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(writer.getId()), club), new ClubMember(new yeobaek.backend.foundation.identity.MemberId(reader.getId()), club)));
    }

    @Test
    @DisplayName("같은 책과 문장의 모임·공개방 댓글은 발견과 상세 조회 상태를 서로 공유하지 않는다")
    void isolateCommentsAndViewStateAcrossSpaces() {
        Long sentenceId = passage.getSentences().getFirst().getId();
        Long clubCommentId = commentService.create(writer.getId(), club.getId(), sentenceId,
                new CommentContent("모임 댓글")).commentId();
        Long roomCommentId = commentService.createInPublicRoom(writer.getId(), publicRoom.getId(), sentenceId,
                new CommentContent("공개방 댓글")).commentId();

        var clubDiscovery = commentService.findCommentedSentences(reader.getId(), club.getId(), passage.getId());
        var roomDiscovery = commentService.findPublicRoomCommentedSentences(
                reader.getId(), publicRoom.getId(), passage.getId());

        assertThat(List.of(
                clubDiscovery.commentedSentences().getFirst().commentCount(),
                roomDiscovery.commentedSentences().getFirst().commentCount(),
                commentService.countNewComments(reader.getId(), club.getId(), passage.getId()).newCommentCount(),
                commentService.countNewPublicRoomComments(
                        reader.getId(), publicRoom.getId(), passage.getId()).newCommentCount(),
                viewRepository.existsByMemberIdAndCommentId(reader.getId(), clubCommentId),
                viewRepository.existsByMemberIdAndCommentId(reader.getId(), roomCommentId)))
                .containsExactly(1L, 1L, 1L, 1L, false, false);

        var clubComments = commentService.findComments(reader.getId(), club.getId(), sentenceId);

        assertThat(List.of(
                clubComments.comments().getFirst().commentId(),
                viewRepository.existsByMemberIdAndCommentId(reader.getId(), clubCommentId),
                viewRepository.existsByMemberIdAndCommentId(reader.getId(), roomCommentId),
                commentService.countNewComments(reader.getId(), club.getId(), passage.getId()).newCommentCount(),
                commentService.countNewPublicRoomComments(
                        reader.getId(), publicRoom.getId(), passage.getId()).newCommentCount()))
                .containsExactly(clubCommentId, true, false, 0L, 1L);

        var roomComments = commentService.findPublicRoomComments(reader.getId(), publicRoom.getId(), sentenceId);

        assertThat(List.of(
                roomComments.comments().getFirst().commentId(),
                commentService.countNewComments(reader.getId(), club.getId(), passage.getId()).newCommentCount(),
                commentService.countNewPublicRoomComments(
                        reader.getId(), publicRoom.getId(), passage.getId()).newCommentCount()))
                .containsExactly(roomCommentId, 0L, 0L);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from appreciations a join comments c on c.id = a.id
                where a.id in (?, ?)
                """, Long.class, clubCommentId, roomCommentId)).isEqualTo(2L);
    }
}
