package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.repository.MemberRepository;

class DevDataSeederTest extends IntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void seedData() {
        new DevDataSeeder(bookRepository, authorRepository, authorBookRepository,
                chapterRepository, passageRepository, memberRepository, clubRepository,
                clubMemberRepository, commentRepository).seed();
    }

    @Test
    @DisplayName("시드는 본문 순서가 1..N으로 빈틈없고 도서의 본문 개수와 일치하도록 투입된다")
    void seedKeepsDenseSequenceContract() {
        Book book = bookRepository.findAll().getFirst();
        List<Integer> sequences = passageRepository.findAll().stream()
                .sorted(Comparator.comparing(Passage::getSequence))
                .map(Passage::getSequence)
                .toList();

        assertThat(book.getId()).isEqualTo(1L);
        assertThat(sequences).hasSize(book.getPassageCount());
        assertThat(sequences.getFirst()).isEqualTo(1);
        assertThat(sequences).doesNotHaveDuplicates();
        assertThat(sequences.getLast()).isEqualTo(book.getPassageCount());
    }

    @Test
    @DisplayName("시드는 고정 ID와 닉네임으로 테스트 회원을 투입한다")
    void seedCreatesMembersWithStableIds() {
        assertThat(memberRepository.findAll(Sort.by("id")))
                .extracting(member -> member.getId(), member -> member.getNickname())
                .containsExactly(tuple(1L, "민서"), tuple(2L, "지수"));
    }

    @Test
    @DisplayName("시드는 고정 참여 코드의 모임과 두 회원의 참여 관계를 투입한다")
    void seedCreatesClubWithMemberships() {
        Club club = clubRepository.findByJoinCode("A3F9KQ").orElseThrow();

        assertThat(club.getId()).isEqualTo(1L);
        assertThat(club.getName()).isEqualTo("교환독서 1기");
        assertThat(clubMemberRepository.findAllJoinedWithMemberByClubId(club.getId()))
                .extracting(clubMember -> clubMember.getMember().getNickname())
                .containsExactly("민서", "지수");
    }

    @Test
    @DisplayName("시드는 회원별 고정 시각과 서로 다른 마지막 본문으로 진도를 투입한다")
    void seedCreatesReadingProgress() {
        Long clubId = clubRepository.findByJoinCode("A3F9KQ").orElseThrow().getId();

        assertThat(clubMemberRepository.findAllJoinedWithMemberByClubId(clubId))
                .extracting(
                        clubMember -> clubMember.getMember().getNickname(),
                        clubMember -> clubMember.getLastReadPassage().getSequence(),
                        clubMember -> clubMember.getLastReadAt())
                .containsExactly(
                        tuple("민서", 10, LocalDateTime.of(2026, 8, 5, 14, 30)),
                        tuple("지수", 20, LocalDateTime.of(2026, 8, 5, 15, 0)));
    }

    @Test
    @DisplayName("시드는 두 회원이 두 번째 본문에 작성한 댓글을 투입한다")
    void seedCreatesCommentsOnSecondPassage() {
        Club club = clubRepository.findByJoinCode("A3F9KQ").orElseThrow();
        Book book = bookRepository.findAll().getFirst();
        Passage passage = passageRepository.findRangeByBookId(book.getId(), 2, 2).getFirst();

        assertThat(passage.getId()).isEqualTo(2L);
        assertThat(passage.getSentences()).extracting("sequence", "content")
                .containsExactly(
                        tuple(1, "(개발용 시드 문단 2) 실제 본문은 인제스트 파이프라인으로 투입된다. "),
                        tuple(2, "이 문단은 읽기 화면·진도·댓글 개발을 위한 자리 채움 텍스트다."));
        assertThat(commentRepository.findAllWithWriterByClubIdAndSentenceId(
                club.getId(), passage.getSentences().getFirst().getId()))
                .extracting(
                        comment -> comment.getClubMember().getMember().getNickname(),
                        comment -> comment.getContent())
                .containsExactly(
                        tuple("민서", "이 문장에서 멈칫했어요."),
                        tuple("지수", "저도 이 대목의 분위기가 오래 남았어요."));
    }
}
