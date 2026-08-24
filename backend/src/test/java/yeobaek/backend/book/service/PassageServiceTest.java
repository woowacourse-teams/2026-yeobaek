package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.repository.BookArchiveRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

class PassageServiceTest extends IntegrationTest {

    @Autowired
    private PassageService passageService;

    @Autowired
    private BookArchiveRepository bookRepository;

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

    private Member reader;
    private Member outsider;
    private Club club;
    private Club otherClub;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 5));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        for (int sequence = 1; sequence <= 5; sequence++) {
            passageRepository.save(new Passage(chapter, sequence, "본문 " + sequence));
        }
        reader = memberRepository.save(new Member("민서"));
        outsider = memberRepository.save(new Member("외부인"));
        club = clubRepository.save(new Club("1기", book, "CODE01"));
        otherClub = clubRepository.save(new Club("2기", book, "CODE02"));
    }

    @Test
    @DisplayName("범위의 본문과 모임 내 댓글 수를 함께 조회한다")
    void findPassagesWithCommentCounts() {
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(reader, club));
        ClubMember otherClubMember = clubMemberRepository.save(new ClubMember(reader, otherClub));
        Passage second = passageRepository.findRangeByBookId(club.getBook().getId(), 2, 2).getFirst();
        commentRepository.save(new Comment(clubMember, second, "우리 모임 댓글 1"));
        commentRepository.save(new Comment(clubMember, second, "우리 모임 댓글 2"));
        commentRepository.save(new Comment(otherClubMember, second, "다른 모임 댓글"));

        PassagesResponse response = passageService.findPassages(reader.getId(), club.getId(), 1, 3);

        assertThat(response.passages()).hasSize(3);
        assertThat(response.passages().get(0).commentCount()).isZero();
        assertThat(response.passages().get(1).sequence()).isEqualTo(2);
        assertThat(response.passages().get(1).commentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("모임에 참여하지 않은 회원의 조회는 거부된다")
    void rejectOutsider() {
        assertThatThrownBy(() -> passageService.findPassages(outsider.getId(), club.getId(), 1, 3))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("탈퇴 회원의 본문 조회는 거부된다")
    void rejectLeftMember() {
        ClubMember membership = clubMemberRepository.save(new ClubMember(reader, club));
        membership.leave();
        clubMemberRepository.saveAndFlush(membership);

        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 1, 3))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("삭제된 도서의 본문 조회를 거부한다")
    void rejectDeletedBook() {
        clubMemberRepository.save(new ClubMember(reader, club));
        Book book = club.getBook();
        bookRepository.delete(book.getId());

        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 1, 3))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("존재하지 않는 모임의 조회는 실패한다")
    void rejectUnknownClub() {
        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), 999L, 1, 3))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("한 번에 100개를 넘는 범위는 거부된다")
    void rejectTooWideRange() {
        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 1, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("시작이 1보다 작거나 끝이 시작보다 앞서는 범위는 거부된다")
    void rejectInvalidRange() {
        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 3, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
