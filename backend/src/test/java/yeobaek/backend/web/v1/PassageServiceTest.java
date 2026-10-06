package yeobaek.backend.web.v1;

import yeobaek.backend.support.CommentFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.domain.Chapter;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.web.book.dto.PassagesResponse;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.space.club.persistence.Club;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.application.content.ContentReadingPolicyException;
import yeobaek.backend.content.api.ContentUnavailableException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.shared.exception.NotFoundException;

class PassageServiceTest extends IntegrationTest {

    @Autowired
    private PassageService passageService;

    @Autowired
    private BookManagementRepository bookRepository;

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

    @Autowired
    private AppreciationContextRepository appreciationContextRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    private Member reader;
    private Member outsider;
    private Book book;
    private Club club;
    private Club otherClub;

    @BeforeEach
    void setUp() {
        book = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), null, 1924, 5, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        for (int sequence = 1; sequence <= 5; sequence++) {
            passageRepository.save(newPassage(chapter, sequence, Collections.singletonList(new SentenceContent("본문 " + sequence))));
        }
        reader = memberRepository.save(new Member(new Nickname("민서")));
        outsider = memberRepository.save(new Member(new Nickname("외부인")));
        club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE01")));
        otherClub = clubRepository.save(newClub(new ClubName("2기"), new JoinCode("CODE02")));
        bindingApi.bind(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId()));
        bindingApi.bind(new SpaceId(otherClub.getSpaceId()), new ContentId(book.getContentId()));
    }

    @Test
    @DisplayName("범위의 본문과 모임 내 댓글 수를 함께 조회한다")
    void findPassagesWithCommentCounts() {
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(reader.getId()), club));
        ClubMember otherClubMember = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(reader.getId()), otherClub));
        Passage second = passageRepository.findRangeByBookId(book.getId(), 2, 2).getFirst();
        var sentence = second.getSentences().getFirst();
        saveComment(newClubComment(clubMember, sentence, new CommentContent("우리 모임 댓글 1")));
        saveComment(newClubComment(clubMember, sentence, new CommentContent("우리 모임 댓글 2")));
        saveComment(newClubComment(otherClubMember, sentence, new CommentContent("다른 모임 댓글")));

        PassagesResponse response = passageService.findPassages(reader.getId(), club.getId(), 1, 3);

        assertThat(response.passages()).hasSize(3);
        assertThat(response.passages().get(0).sentences().getFirst().commentCount()).isZero();
        assertThat(response.passages().get(1).sequence()).isEqualTo(2);
        assertThat(response.passages().get(1).sentences().getFirst().commentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("댓글 신고 전후 본문 응답의 댓글 수는 동일하다")
    void preserveCommentCountAfterReport() {
        Member writer = memberRepository.save(new Member(new Nickname("작성자")));
        clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(reader.getId()), club));
        ClubMember writerMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(writer.getId()), club));
        Passage first = passageRepository.findRangeByBookId(book.getId(), 1, 1).getFirst();
        Comment comment = saveComment(
                newClubComment(writerMembership, first.getSentences().getFirst(), new CommentContent("신고 대상")));

        PassagesResponse beforeReport = passageService.findPassages(reader.getId(), club.getId(), 1, 1);
        commentService.report(reader.getId(), comment.getId());
        PassagesResponse afterReport = passageService.findPassages(reader.getId(), club.getId(), 1, 1);

        assertThat(beforeReport.passages().getFirst().sentences().getFirst().commentCount()).isEqualTo(1);
        assertThat(afterReport.passages().getFirst().sentences().getFirst().commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("문장 댓글 수에서 요청자가 차단한 회원의 댓글을 제외한다")
    void excludeBlockedWritersFromCommentCounts() {
        Member blockedWriter = memberRepository.save(new Member(new Nickname("차단 대상")));
        ClubMember readerMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(reader.getId()), club));
        ClubMember blockedMembership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(blockedWriter.getId()), club));
        Passage first = passageRepository.findRangeByBookId(book.getId(), 1, 1).getFirst();
        var sentence = first.getSentences().getFirst();
        saveComment(newClubComment(readerMembership, sentence, new CommentContent("보이는 댓글")));
        saveComment(newClubComment(blockedMembership, sentence, new CommentContent("숨길 댓글")));
        memberBlockRepository.save(new MemberBlock(reader, blockedWriter));

        PassagesResponse response = passageService.findPassages(reader.getId(), club.getId(), 1, 1);

        assertThat(response.passages().getFirst().sentences().getFirst().commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("모임에 참여하지 않은 회원의 조회는 거부된다")
    void rejectOutsider() {
        assertThatThrownBy(() -> passageService.findPassages(outsider.getId(), club.getId(), 1, 3))
                .isInstanceOfSatisfying(ContentReadingPolicyException.class,
                        failure -> assertThat(failure.getCode())
                                .isEqualTo(ErrorCode.SPACE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("탈퇴 회원의 본문 조회는 거부된다")
    void rejectLeftMember() {
        ClubMember membership = clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(reader.getId()), club));
        membership.leave();
        clubMemberRepository.saveAndFlush(membership);

        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 1, 3))
                .isInstanceOfSatisfying(ContentReadingPolicyException.class,
                        failure -> assertThat(failure.getCode())
                                .isEqualTo(ErrorCode.SPACE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("삭제된 도서를 대상으로 본문을 조회할 수 없다")
    void cannotReadPassagesOfDeletedBook() {
        clubMemberRepository.save(new ClubMember(new yeobaek.backend.shared.identity.MemberId(reader.getId()), club));
        bookRepository.delete(book.getId());

        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 1, 3))
                .isInstanceOf(ContentUnavailableException.class);
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
                .isInstanceOfSatisfying(ContentReadingPolicyException.class,
                        failure -> {
                            assertThat(failure.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                            assertThat(failure.getLogContext()).containsEntry("reason", "RANGE_TOO_LARGE");
                        });
    }

    @Test
    @DisplayName("시작이 1보다 작거나 끝이 시작보다 앞서는 범위는 거부된다")
    void rejectInvalidRange() {
        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> passageService.findPassages(reader.getId(), club.getId(), 3, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Comment saveComment(Comment comment) {
        Comment saved = commentRepository.save(comment);
        appreciationContextRepository.save(CommentFixtures.contextOf(saved));
        return saved;
    }
}
