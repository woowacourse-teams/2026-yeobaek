package watson.backend.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import watson.backend.book.Book;
import watson.backend.book.BookRepository;
import watson.backend.member.Member;
import watson.backend.member.MemberRepository;
import watson.backend.support.RepositoryTest;

class ClubMappingTest extends RepositoryTest {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    private Book saveBook() {
        return bookRepository.save(new Book("운수 좋은 날", null, 1924, 10));
    }

    @Test
    @DisplayName("모임을 저장하면 도서와 참여 코드가 함께 조회된다")
    void saveAndFind() {
        Club saved = clubRepository.save(new Club("교환독서 1기", saveBook(), "A3F9KQ"));

        Club found = clubRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getJoinCode()).isEqualTo("A3F9KQ");
        assertThat(found.getBook().getTitle()).isEqualTo("운수 좋은 날");
    }

    @Test
    @DisplayName("참여 코드가 중복되면 저장에 실패한다")
    void duplicateJoinCodeRejected() {
        Book book = saveBook();
        clubRepository.saveAndFlush(new Club("1기", book, "SAME01"));

        assertThatThrownBy(() -> clubRepository.saveAndFlush(new Club("2기", book, "SAME01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 회원이 같은 모임에 두 번 참여하면 저장에 실패한다")
    void duplicateParticipationRejected() {
        Member member = memberRepository.save(new Member("민서"));
        Club club = clubRepository.save(new Club("1기", saveBook(), "CODE01"));
        clubMemberRepository.saveAndFlush(new ClubMember(member, club));

        assertThatThrownBy(() -> clubMemberRepository.saveAndFlush(new ClubMember(member, club)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("모임 참여 직후에는 최근 열람 본문과 마지막 읽은 시간이 비어 있다")
    void progressStartsEmpty() {
        Member member = memberRepository.save(new Member("민서"));
        Club club = clubRepository.save(new Club("1기", saveBook(), "CODE02"));

        ClubMember saved = clubMemberRepository.save(new ClubMember(member, club));

        assertThat(saved.getLastReadPassage()).isNull();
        assertThat(saved.getLastReadAt()).isNull();
    }
}
