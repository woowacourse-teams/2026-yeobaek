package yeobaek.backend.survey.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.DatabaseCleaningIntegrationTest;

class SurveyHistoryPersistenceTest extends DatabaseCleaningIntegrationTest {

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
    private PublicRoomRepository publicRoomRepository;

    @Test
    @DisplayName("댓글 조회 기록이 저장되면 설문 대상이 되고 유일한 댓글 삭제 후에는 대상에서 제외된다")
    void surveyEligibilityFollowsPersistedHistoryAndCascadeDeletion() {
        Book book = bookRepository.save(new Book(new BookTitle("설문 도서"), null, 1924, 1,
                null, null, null, List.of(), List.of()));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("본문"))));
        Long sentenceId = passage.getSentences().getFirst().getId();
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        var comment = commentService.createInPublicRoom(writer.getId(), room.getId(), sentenceId,
                new CommentContent("설문 기록을 남기는 댓글"));
        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isFalse();
        assertThat(surveyService.getSurveyRequirement(writer.getId()).surveyRequired()).isTrue();

        commentService.findPublicRoomComments(reader.getId(), room.getId(), sentenceId);

        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isTrue();

        commentService.delete(writer.getId(), comment.commentId());

        assertThat(surveyService.getSurveyRequirement(reader.getId()).surveyRequired()).isFalse();
        assertThat(surveyService.getSurveyRequirement(writer.getId()).surveyRequired()).isFalse();
    }
}
