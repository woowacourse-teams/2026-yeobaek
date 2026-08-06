package watson.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.book.domain.Book;
import watson.backend.book.repository.BookRepository;
import watson.backend.club.domain.Club;
import watson.backend.club.domain.JoinCodeGenerator;
import watson.backend.club.dto.ClubCreateResponse;
import watson.backend.club.repository.ClubRepository;
import watson.backend.member.domain.Member;
import watson.backend.member.repository.MemberRepository;
import watson.backend.support.RepositoryTest;

@Import(ClubService.class)
class ClubJoinCodeCollisionTest extends RepositoryTest {

    private static final String TAKEN_CODE = "TAKEN1";

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
        clubRepository.save(new Club("기존 모임", book, TAKEN_CODE));
    }

    @Test
    @DisplayName("발급된 코드가 이미 존재하면 재생성해 유일한 코드를 발급한다")
    void regenerateOnCollision() {
        given(joinCodeGenerator.generate()).willReturn(TAKEN_CODE, TAKEN_CODE, "FRESH1");

        ClubCreateResponse response = clubService.create(creator.getId(), "새 모임", book.getId());

        assertThat(response.joinCode()).isEqualTo("FRESH1");
    }

    @Test
    @DisplayName("5회 연속 충돌하면 서버 에러로 처리한다")
    void failAfterFiveCollisions() {
        given(joinCodeGenerator.generate()).willReturn(TAKEN_CODE);

        assertThatThrownBy(() -> clubService.create(creator.getId(), "새 모임", book.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
