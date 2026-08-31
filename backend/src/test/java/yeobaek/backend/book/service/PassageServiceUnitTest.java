package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
class PassageServiceUnitTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private PassageRepository passageRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PassageService passageService;

    @Test
    @DisplayName("조회된 문장이 없으면 댓글 수 집계 쿼리를 실행하지 않는다")
    void skipCommentCountQueryWhenNoSentencesAreFound() {
        Book book = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 20L);
        Club club = new Club("모임", book, "CODE01");
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(clubMemberRepository.existsJoinedByMemberIdAndClubId(1L, 10L)).willReturn(true);
        given(passageRepository.findRangeByBookId(20L, 1, 10)).willReturn(List.of());

        var response = passageService.findPassages(1L, 10L, 1, 10);

        assertThat(response.passages()).isEmpty();
        verifyNoInteractions(commentRepository);
    }
}
