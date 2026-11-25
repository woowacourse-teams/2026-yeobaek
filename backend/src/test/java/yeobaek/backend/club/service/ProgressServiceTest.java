package yeobaek.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.InvalidRequestException;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private AuthorBookRepository authorBookRepository;
    @Mock
    private BookCoverUrlResolver bookCoverUrlResolver;
    @InjectMocks
    private ProgressService progressService;

    private Book book;
    private Club club;
    private ClubMember membership;
    private Passage second;

    @BeforeEach
    void setUp() {
        book = new Book(new BookTitle("운수 좋은 날"), null, null, 4, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 20L);
        club = new Club(new ClubName("독서 모임"), book, new JoinCode("CODE01"));
        ReflectionTestUtils.setField(club, "id", 10L);
        membership = new ClubMember(new Member(new Nickname("독자")), club);
        second = new Passage(new Chapter(book, new ChapterTitle("1장"), 1), 2,
                List.of(new SentenceContent("두 번째 본문")));
    }

    @Test
    @DisplayName("다시 앞부분을 읽으면 저장 위치와 진도율도 뒤로 이동한다")
    void rereadingEarlierPassage() {
        Passage fourth = new Passage(second.getChapter(), 4, List.of(new SentenceContent("마지막 본문")));
        membership.updateProgress(fourth, LocalDateTime.of(2026, 1, 1, 12, 0));
        givenTargets(second);
        LocalDateTime before = LocalDateTime.now();

        var response = progressService.updateProgress(1L, 10L, 30L);

        assertThat(response.lastReadPassageSequence()).isEqualTo(2);
        assertThat(response.progressRate()).isEqualTo(50);
        assertThat(response.lastReadAt()).isBetween(before, LocalDateTime.now());
        assertThat(membership.getLastReadPassage()).isSameAs(second);
    }

    @Test
    @DisplayName("현재 모임 참여자가 아니면 진도를 변경할 수 없다")
    void rejectNonParticipant() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));

        assertThatThrownBy(() -> progressService.updateProgress(1L, 10L, 30L))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
        assertThat(membership.getLastReadPassage()).isNull();
    }

    @Test
    @DisplayName("다른 도서의 본문은 현재 모임 진도에 저장하지 않는다")
    void rejectOtherBook() {
        Book other = new Book(new BookTitle("다른 책"), null, null, 1, null, null, null, null, null);
        ReflectionTestUtils.setField(other, "id", 21L);
        Passage otherPassage = new Passage(new Chapter(other, new ChapterTitle("첫 장"), 1), 1,
                List.of(new SentenceContent("다른 본문")));
        givenTargets(otherPassage);

        assertThatThrownBy(() -> progressService.updateProgress(1L, 10L, 30L))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(membership.getLastReadPassage()).isNull();
    }

    @Test
    @DisplayName("삭제된 책의 진도는 갱신할 수 없다")
    void rejectDeletedBook() {
        book.delete();
        givenTargets(second);

        assertThatThrownBy(() -> progressService.updateProgress(1L, 10L, 30L))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
        assertThat(membership.getLastReadPassage()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 모임은 모임 오류로 구분한다")
    void missingClub() {
        assertThatThrownBy(() -> progressService.updateProgress(1L, 10L, 30L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CLUB_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않는 본문은 본문 오류로 구분한다")
    void missingPassage() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(clubMemberRepository.findJoinedByMemberIdAndClubId(1L, 10L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> progressService.updateProgress(1L, 10L, 30L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.PASSAGE_NOT_FOUND));
    }

    @Test
    @DisplayName("읽은 기록이 없으면 마지막 읽기도 없다")
    void noReadingHistory() {
        assertThat(progressService.findLastReading(1L)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 도서의 마지막 읽기도 상태와 저장 위치를 보존한다")
    void preserveDeletedBookHistory() {
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        membership.updateProgress(second, readAt);
        book.delete();
        given(clubMemberRepository.findAllJoinedWithLastReadingByMemberId(1L)).willReturn(List.of(membership));

        var response = progressService.findLastReading(1L).orElseThrow();

        assertThat(response.book().status()).isEqualTo(BookStatus.DELETED);
        assertThat(response.lastReadPassageSequence()).isEqualTo(2);
        assertThat(response.lastReadAt()).isEqualTo(readAt);
    }

    private void givenTargets(Passage passage) {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(clubMemberRepository.findJoinedByMemberIdAndClubId(1L, 10L)).willReturn(Optional.of(membership));
        given(passageRepository.findById(30L)).willReturn(Optional.of(passage));
    }
}
