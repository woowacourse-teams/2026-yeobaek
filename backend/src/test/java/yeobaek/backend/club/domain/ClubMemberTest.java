package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

class ClubMemberTest {

    @Test
    @DisplayName("모임 참여 정보는 참여 중 상태로 생성된다")
    void statusStartsJoined() {
        ClubMember clubMember = new ClubMember(
                new Member(new Nickname("민서")), new Club(new ClubName("1기"), new Book(new BookTitle("제목"), null, null, 1, null, null, null), new JoinCode("CODE01")));

        assertThat(clubMember.getStatus()).isEqualTo(ClubMemberStatus.JOINED);
        assertThat(clubMember.isJoined()).isTrue();
    }

    @Test
    @DisplayName("모임을 탈퇴한 후 재가입할 수 있다")
    void leaveAndRejoin() {
        ClubMember clubMember = new ClubMember(
                new Member(new Nickname("민서")), new Club(new ClubName("1기"), new Book(new BookTitle("제목"), null, null, 1, null, null, null), new JoinCode("CODE01")));

        clubMember.leave();

        assertThat(clubMember.getStatus()).isEqualTo(ClubMemberStatus.LEFT);
        assertThat(clubMember.isJoined()).isFalse();

        clubMember.rejoin();

        assertThat(clubMember.getStatus()).isEqualTo(ClubMemberStatus.JOINED);
        assertThat(clubMember.isJoined()).isTrue();
    }

    @Test
    @DisplayName("본인의 회원 id를 전달하면 참을 반환한다")
    void isOwnedByWhenIdMatches() {
        Member member = new Member(new Nickname("민서"));
        ReflectionTestUtils.setField(member, "id", 1L);
        ClubMember clubMember = new ClubMember(member, new Club(new ClubName("1기"), new Book(new BookTitle("제목"), null, null, 1, null, null, null), new JoinCode("CODE01")));

        assertThat(clubMember.isOwnedBy(1L)).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 id를 전달하면 거짓을 반환한다")
    void isNotOwnedByWhenIdDiffers() {
        Member member = new Member(new Nickname("민서"));
        ReflectionTestUtils.setField(member, "id", 1L);
        ClubMember clubMember = new ClubMember(member, new Club(new ClubName("1기"), new Book(new BookTitle("제목"), null, null, 1, null, null, null), new JoinCode("CODE01")));

        assertThat(clubMember.isOwnedBy(2L)).isFalse();
    }

    @Test
    @DisplayName("진도율은 최근 열람 본문 순서를 도서의 본문 개수로 나눈 값을 반올림한다")
    void progressRateDelegatesTotalPassageCountToClub() {
        Book book = new Book(new BookTitle("제목"), null, null, 4, null, null, null);
        Chapter chapter = new Chapter(book, new ChapterTitle("1장"), 1);
        Passage passage = new Passage(chapter, 3, Collections.singletonList(new SentenceContent("본문")));
        Club club = new Club(new ClubName("1기"), book, new JoinCode("CODE01"));
        ClubMember clubMember = new ClubMember(new Member(new Nickname("민서")), club);

        clubMember.updateProgress(passage, LocalDateTime.now());

        assertThat(clubMember.progressRate()).isEqualTo(75);
    }

    @Test
    @DisplayName("참여 중인 회원은 모임의 이용 가능한 도서에 속한 문단을 읽을 수 있다")
    void ensureCanReadJoinedClubBookPassage() {
        Book book = book(1L);
        ClubMember clubMember = clubMember(book);

        assertThatCode(() -> clubMember.ensureCanRead(passage(book, 11L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("탈퇴한 회원은 모임 도서의 문단을 읽을 수 없다")
    void ensureCanReadRejectsLeftMember() {
        Book book = book(1L);
        ClubMember clubMember = clubMember(book);
        clubMember.leave();

        assertThatThrownBy(() -> clubMember.ensureCanRead(passage(book, 11L)))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
    }

    @Test
    @DisplayName("재가입한 회원은 다시 모임 도서의 문단을 읽을 수 있다")
    void ensureCanReadAllowsRejoinedMember() {
        Book book = book(1L);
        ClubMember clubMember = clubMember(book);
        clubMember.leave();
        clubMember.rejoin();

        assertThatCode(() -> clubMember.ensureCanRead(passage(book, 11L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 도서에 속한 문단은 읽을 수 없다")
    void ensureCanReadRejectsOtherBookPassage() {
        Book clubBook = book(1L);
        Book otherBook = book(2L);
        ClubMember clubMember = clubMember(clubBook);

        assertThatThrownBy(() -> clubMember.ensureCanRead(passage(otherBook, 11L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("삭제된 도서에 속한 문단은 읽을 수 없다")
    void ensureCanReadRejectsDeletedBookPassage() {
        Book book = book(1L);
        ClubMember clubMember = clubMember(book);
        Passage passage = passage(book, 11L);
        book.delete();

        assertThatThrownBy(() -> clubMember.ensureCanRead(passage))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("탈퇴 상태를 문단 소속과 도서 가용성보다 먼저 검증한다")
    void ensureCanReadChecksJoinedStatusFirst() {
        Book clubBook = book(1L);
        Book otherBook = book(2L);
        ClubMember clubMember = clubMember(clubBook);
        clubMember.leave();
        clubBook.delete();

        assertThatThrownBy(() -> clubMember.ensureCanRead(passage(otherBook, 11L)))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
    }

    private ClubMember clubMember(Book book) {
        Member member = new Member(new Nickname("민서"));
        ReflectionTestUtils.setField(member, "id", 7L);
        Club club = new Club(new ClubName("1기"), book, new JoinCode("CODE01"));
        ReflectionTestUtils.setField(club, "id", 5L);
        return new ClubMember(member, club);
    }

    private Book book(Long id) {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Passage passage(Book book, Long id) {
        Chapter chapter = new Chapter(book, new ChapterTitle("1장"), 1);
        Passage passage = new Passage(chapter, 1, Collections.singletonList(new SentenceContent("본문")));
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }
}
