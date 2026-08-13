package yeobaek.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.BookRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.JoinCodeGenerator;
import yeobaek.backend.club.dto.ClubCreateResponse;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.RepositoryTest;

@Import(ClubService.class)
class ClubJoinCodeCollisionTest extends RepositoryTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private JoinCodeGenerator joinCodeGenerator;

    private Member creator;
    private Book book;

    @BeforeEach
    void setUp() {
        creator = memberRepository.save(new Member("민서"));
        book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 312));
        clubRepository.save(new Club("기존 모임", book, "TAKEN1"));
    }

    @Test
    @DisplayName("발급된 코드가 이미 존재하면 재생성해 유일한 코드를 발급한다")
    void regenerateOnCollision() {
        given(joinCodeGenerator.generate()).willReturn("TAKEN1", "TAKEN1", "FRESH1");

        ClubCreateResponse response = clubService.create(creator.getId(), "새 모임", book.getId());

        assertThat(response.joinCode()).isEqualTo("FRESH1");
    }

    @Test
    @DisplayName("5회 연속 충돌하면 서버 에러로 처리한다")
    void failAfterFiveCollisions() {
        given(joinCodeGenerator.generate()).willReturn("TAKEN1");

        assertThatThrownBy(() -> clubService.create(creator.getId(), "새 모임", book.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
