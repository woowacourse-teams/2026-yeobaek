package yeobaek.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class SurveyServiceTest extends IntegrationTest {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private CommentService commentService;

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
    private PublicRoomRepository publicRoomRepository;

    private Member writer;
    private Member reader;
    private Club club;
    private PublicRoom publicRoom;
    private Sentence sentence;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book(new BookTitle("설문 테스트 도서"), null, 1924, 1, null, null, null, List.of(), List.of()));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("본문"))));
        sentence = passage.getSentences().getFirst();
        writer = memberRepository.save(new Member(new Nickname("작성자")));
        reader = memberRepository.save(new Member(new Nickname("독자")));
        club = clubRepository.save(new Club(new ClubName("설문 모임"), book, new JoinCode("SURV01")));
        clubMemberRepository.save(new ClubMember(writer, club));
        clubMemberRepository.save(new ClubMember(reader, club));
        publicRoom = publicRoomRepository.save(new PublicRoom(book));
    }

    @Test
    @DisplayName("댓글 조회 기록이 없으면 설문 대상이 아니다")
    void noViews() {
        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isFalse();
    }

    @Test
    @DisplayName("댓글을 작성하기만 해도 본인 조회 기록에 따라 설문 대상이 된다")
    void createdOwnComment() {
        commentService.create(reader.getId(), club.getId(), sentence.getId(), new CommentContent("내 모임 댓글"));

        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isTrue();
        assertThat(surveyService.getSurveyRequirement(writer.getId()).surveyRequired()).isFalse();
    }

    @Test
    @DisplayName("모임에서 댓글을 작성하거나 조회한 회원은 설문 대상이 된다")
    void viewedClubComment() {
        commentService.create(writer.getId(), club.getId(), sentence.getId(), new CommentContent("모임 댓글"));
        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isFalse();

        commentService.findComments(reader.getId(), club.getId(), sentence.getId());

        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isTrue();
        assertThat(surveyService.getSurveyRequirement(writer.getId()).surveyRequired()).isTrue();
    }

    @Test
    @DisplayName("공개방에서 댓글을 작성하거나 조회한 회원은 설문 대상이 된다")
    void viewedPublicRoomComment() {
        commentService.createInPublicRoom(writer.getId(), publicRoom.getId(), sentence.getId(),
                new CommentContent("공개방 댓글"));
        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isFalse();

        commentService.findPublicRoomComments(reader.getId(), publicRoom.getId(), sentence.getId());

        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isTrue();
        assertThat(surveyService.getSurveyRequirement(writer.getId()).surveyRequired()).isTrue();
    }

    @Test
    @DisplayName("유일하게 조회한 타인 댓글이 삭제되면 설문 대상에서 제외된다")
    void deletedViewedComment() {
        var comment = commentService.createInPublicRoom(writer.getId(), publicRoom.getId(), sentence.getId(),
                new CommentContent("삭제할 댓글"));
        commentService.findPublicRoomComments(reader.getId(), publicRoom.getId(), sentence.getId());
        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isTrue();

        commentService.delete(writer.getId(), comment.commentId());

        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isFalse();
    }
}
