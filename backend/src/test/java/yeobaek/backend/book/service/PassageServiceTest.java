package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.SentenceCommentCount;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class PassageServiceTest {

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private SentenceCommentCount commentCount;
    @InjectMocks
    private PassageService passageService;

    private Book book;
    private Club club;

    @BeforeEach
    void setUp() {
        book = new Book(new BookTitle("읽는 책"), null, null, 100, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 20L);
        club = new Club(new ClubName("독서 모임"), book, new JoinCode("CODE01"));
    }

    @Test
    @DisplayName("문장별 집계가 없는 문장은 0으로 반환하고 집계된 댓글 수는 해당 문장에 연결한다")
    void sentenceCounts() {
        givenParticipant();
        Chapter chapter = new Chapter(book, new ChapterTitle("첫 장"), 1);
        ReflectionTestUtils.setField(chapter, "id", 30L);
        Passage passage = new Passage(chapter, 1, List.of(new SentenceContent("첫 문장"), new SentenceContent("둘째 문장")));
        ReflectionTestUtils.setField(passage.getSentences().get(0), "id", 41L);
        ReflectionTestUtils.setField(passage.getSentences().get(1), "id", 42L);
        given(passageRepository.findRangeByBookId(20L, 1, 100)).willReturn(List.of(passage));
        given(commentRepository.countVisibleByMemberIdAndClubIdAndSentenceIdIn(1L, 10L, List.of(41L, 42L)))
                .willReturn(List.of(commentCount));
        given(commentCount.getSentenceId()).willReturn(42L);
        given(commentCount.getCommentCount()).willReturn(3L);

        var response = passageService.findPassages(1L, 10L, 1, 100);

        assertThat(response.passages()).singleElement().satisfies(result -> {
            assertThat(result.chapterId()).isEqualTo(30L);
            assertThat(result.sentences()).extracting(sentence -> sentence.commentCount()).containsExactly(0L, 3L);
        });
    }

    @Test
    @DisplayName("범위에 본문이 없으면 빈 배열을 반환한다")
    void emptyRange() {
        givenParticipant();

        assertThat(passageService.findPassages(1L, 10L, 1, 10).passages()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0,3", "3,2", "1,101"})
    @DisplayName("잘못된 범위와 100개를 초과한 범위는 거부한다")
    void rejectInvalidRange(int from, int to) {
        assertThatThrownBy(() -> passageService.findPassages(1L, 10L, from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("참여 중인 회원이 아니면 본문을 읽을 수 없다")
    void rejectNonParticipant() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));

        assertThatThrownBy(() -> passageService.findPassages(1L, 10L, 1, 10))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
    }

    @Test
    @DisplayName("없는 모임의 본문 조회는 CLUB_NOT_FOUND로 거부한다")
    void rejectMissingClub() {
        assertThatThrownBy(() -> passageService.findPassages(1L, 10L, 1, 10))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CLUB_NOT_FOUND));
    }

    @Test
    @DisplayName("삭제 도서의 본문은 반환하지 않는다")
    void rejectDeletedBook() {
        givenParticipant();
        book.delete();

        assertThatThrownBy(() -> passageService.findPassages(1L, 10L, 1, 10))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
    }

    private void givenParticipant() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(clubMemberRepository.existsJoinedByMemberIdAndClubId(1L, 10L)).willReturn(true);
    }
}
