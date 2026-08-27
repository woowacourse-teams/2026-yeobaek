package yeobaek.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.JoinCodeGenerator;
import yeobaek.backend.club.dto.ClubCreateResponse;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class ClubJoinCodeCollisionTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long BOOK_ID = 2L;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ActiveBookRepository bookRepository;

    @Mock
    private AuthorBookRepository authorBookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JoinCodeGenerator joinCodeGenerator;

    @Mock
    private BookCoverUrlResolver bookCoverUrlResolver;

    @Mock
    private Book book;

    @Mock
    private Member member;

    @InjectMocks
    private ClubService clubService;

    @Test
    @DisplayName("발급된 코드가 이미 존재하면 재생성해 유일한 코드를 발급한다")
    void regenerateOnCollision() {
        given(bookRepository.getById(BOOK_ID)).willReturn(book);
        given(book.getId()).willReturn(BOOK_ID);
        given(book.getTitle()).willReturn("운수 좋은 날");
        given(book.getPassageCount()).willReturn(312);
        given(joinCodeGenerator.generate()).willReturn("TAKEN1", "TAKEN1", "FRESH1");
        given(clubRepository.existsByJoinCode("TAKEN1")).willReturn(true);
        given(clubRepository.existsByJoinCode("FRESH1")).willReturn(false);
        given(clubRepository.save(any(Club.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(memberRepository.getReferenceById(MEMBER_ID)).willReturn(member);
        given(clubMemberRepository.save(any(ClubMember.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(authorBookRepository.findAllWithAuthorByBookIdIn(List.of(BOOK_ID))).willReturn(List.of());

        ClubCreateResponse response = clubService.create(MEMBER_ID, "새 모임", BOOK_ID);

        assertThat(response.joinCode()).isEqualTo("FRESH1");
    }

    @Test
    @DisplayName("5회 연속 충돌하면 서버 에러로 처리한다")
    void failAfterFiveCollisions() {
        given(bookRepository.getById(BOOK_ID)).willReturn(book);
        given(joinCodeGenerator.generate()).willReturn("TAKEN1");
        given(clubRepository.existsByJoinCode("TAKEN1")).willReturn(true);

        assertThatThrownBy(() -> clubService.create(MEMBER_ID, "새 모임", BOOK_ID))
                .isInstanceOf(IllegalStateException.class);
    }
}
