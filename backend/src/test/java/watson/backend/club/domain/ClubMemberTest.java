package watson.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Chapter;
import watson.backend.book.domain.Passage;
import watson.backend.member.domain.Member;

class ClubMemberTest {

    @Test
    @DisplayName("본인의 회원 id를 전달하면 참을 반환한다")
    void isMemberWhenIdMatches() {
        Member member = new Member("민서");
        ReflectionTestUtils.setField(member, "id", 1L);
        ClubMember clubMember = new ClubMember(member, new Club("1기", new Book("제목", null, null, 1), "CODE01"));

        assertThat(clubMember.isMember(1L)).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 id를 전달하면 거짓을 반환한다")
    void isNotMemberWhenIdDiffers() {
        Member member = new Member("민서");
        ReflectionTestUtils.setField(member, "id", 1L);
        ClubMember clubMember = new ClubMember(member, new Club("1기", new Book("제목", null, null, 1), "CODE01"));

        assertThat(clubMember.isMember(2L)).isFalse();
    }

    @Test
    @DisplayName("진도율은 최근 열람 본문 순서를 도서의 본문 개수로 나눈 값을 반올림한다")
    void progressRateDelegatesTotalPassageCountToClub() {
        Book book = new Book("제목", null, null, 4);
        Chapter chapter = new Chapter(book, "1장", 1);
        Passage passage = new Passage(chapter, 3, "본문", null);
        Club club = new Club("1기", book, "CODE01");
        ClubMember clubMember = new ClubMember(new Member("민서"), club);

        clubMember.updateProgress(passage, LocalDateTime.now());

        assertThat(clubMember.progressRate()).isEqualTo(75);
    }
}
