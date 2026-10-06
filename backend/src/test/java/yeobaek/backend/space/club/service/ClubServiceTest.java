package yeobaek.backend.space.club.service;

import yeobaek.backend.web.v1.ClubService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.content.book.domain.Author;
import yeobaek.backend.content.book.domain.AuthorBook;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.web.common.dto.BookResponse.Status;
import yeobaek.backend.content.book.domain.Chapter;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.api.value.AuthorName;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.Publisher;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.content.book.repository.AuthorBookRepository;
import yeobaek.backend.content.book.repository.AuthorRepository;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.club.domain.ClubMemberStatus;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.web.club.dto.ClubCreateResponse;
import yeobaek.backend.web.club.dto.ClubDetailResponse;
import yeobaek.backend.web.club.dto.ClubJoinResponse;
import yeobaek.backend.web.club.dto.ClubMemberResponse;
import yeobaek.backend.web.club.dto.MyClubResponse;
import yeobaek.backend.web.club.dto.MyClubsResponse;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.content.api.ContentUnavailableException;
import yeobaek.backend.application.space.query.SpaceQueryPolicyException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.ForbiddenException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.shared.exception.NotFoundException;

class ClubServiceTest extends IntegrationTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private ReadingProgressApi readingProgressApi;

    private Member creator;
    private Book book;

    @BeforeEach
    void setUp() {
        creator = memberRepository.save(new Member(new Nickname("민서")));
        book = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 312, null));
        Author author = authorRepository.save(new Author(new AuthorName("현진건")));
        authorBookRepository.save(new AuthorBook(author, book));
    }

    @Nested
    @DisplayName("도서를 읽는 모임을 새로 만들거나 참여할 수 있는가")
    class CreateOrJoinBookClub {

        @Test
        @DisplayName("삭제된 도서를 읽는 모임은 새로 만들 수 없다")
        void cannotCreateClubForDeletedBook() {
            bookRepository.delete(book.getId());

            assertThatThrownBy(() -> clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId()))
                    .isInstanceOf(ContentUnavailableException.class);
        }

        @Test
        @DisplayName("삭제된 도서를 읽는 기존 모임에는 새로 참여할 수 없다")
        void cannotJoinClubForDeletedBook() {
            ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), book.getId());
            Member joiner = memberRepository.save(new Member(new Nickname("지수")));
            bookRepository.delete(book.getId());

            assertThatThrownBy(() -> clubService.join(joiner.getId(), new JoinCode(created.joinCode())))
                    .isInstanceOf(ContentUnavailableException.class);
            assertThat(clubMemberRepository.findByMemberIdAndClubId(joiner.getId(), created.clubId())).isEmpty();
        }
    }

    @Test
    @DisplayName("모임을 생성하면 참여 코드가 발급되고 생성자가 자동으로 참여한다")
    void createClub() {
        ClubCreateResponse response = clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId());

        assertThat(response.joinCode()).matches("^[A-Z0-9]{6}$");
        assertThat(response.book().authors()).containsExactly("현진건");
        assertThat(clubRepository.findById(response.clubId())).isPresent();
        assertThat(clubMemberRepository.existsJoinedByMemberIdAndClubId(creator.getId(), response.clubId())).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 도서로는 모임을 생성할 수 없다")
    void rejectUnknownBook() {
        assertThatThrownBy(() -> clubService.create(creator.getId(), new ClubName("교환독서 1기"), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("참여 코드로 모임에 참여한다")
    void joinByCode() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId());
        Member joiner = memberRepository.save(new Member(new Nickname("지수")));

        ClubJoinResponse response = clubService.join(joiner.getId(), new JoinCode(created.joinCode()));

        assertThat(response.clubId()).isEqualTo(created.clubId());
        assertThat(clubMemberRepository.existsJoinedByMemberIdAndClubId(joiner.getId(), created.clubId())).isTrue();
    }

    @Test
    @DisplayName("형식이 유효하지만 존재하지 않는 참여 코드는 JOIN_CODE_NOT_FOUND로 처리한다")
    void unknownJoinCodeIsNotFound() {
        assertThatThrownBy(() -> clubService.join(creator.getId(), new JoinCode("ABC123")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.JOIN_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("이미 참여한 모임에 다시 참여해도 같은 응답을 반환한다 (멱등)")
    void joinIsIdempotent() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId());

        ClubJoinResponse response = clubService.join(creator.getId(), new JoinCode(created.joinCode()));

        assertThat(response.clubId()).isEqualTo(created.clubId());
        assertThat(clubMemberRepository.countJoinedMembersByClubIds(List.of(created.clubId()))
                .getFirst().getMemberCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("모임을 탈퇴하면 참여 정보를 탈퇴 상태로 바꾸고 중복 요청도 성공한다")
    void leaveIsIdempotent() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId());

        clubService.leave(creator.getId(), created.clubId());
        clubService.leave(creator.getId(), created.clubId());

        ClubMember membership = clubMemberRepository
                .findByMemberIdAndClubId(creator.getId(), created.clubId()).orElseThrow();
        assertThat(membership.getStatus()).isEqualTo(ClubMemberStatus.LEFT);
        assertThat(clubMemberRepository.existsJoinedByMemberIdAndClubId(creator.getId(), created.clubId())).isFalse();
    }

    @Test
    @DisplayName("가입 이력이 없는 회원은 모임을 탈퇴할 수 없다")
    void rejectLeaveWithoutHistory() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId());
        Member outsider = memberRepository.save(new Member(new Nickname("외부인")));

        assertThatThrownBy(() -> clubService.leave(outsider.getId(), created.clubId()))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
    }

    @Test
    @DisplayName("존재하지 않는 모임은 탈퇴할 수 없다")
    void rejectLeaveFromUnknownClub() {
        assertThatThrownBy(() -> clubService.leave(creator.getId(), 999L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CLUB_NOT_FOUND));
    }

    @Test
    @DisplayName("탈퇴 후 재가입하면 기존 참여 정보와 진도를 복구한다")
    void rejoinRestoresMembershipAndProgress() {
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 42, Collections.singletonList(new SentenceContent("본문"))));
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("교환독서 1기"), book.getId());
        ClubMember membership = clubMemberRepository
                .findByMemberIdAndClubId(creator.getId(), created.clubId()).orElseThrow();
        LocalDateTime lastReadAt = LocalDateTime.of(2026, 8, 24, 12, 0);
        var progressClub = clubRepository.findById(created.clubId()).orElseThrow();
        readingProgressApi.update(new MemberId(creator.getId()), new SpaceId(progressClub.getSpaceId()),
                new ContentId(book.getContentId()), new ContentLocationId(passage.getLocationId()),
                lastReadAt);

        clubService.leave(creator.getId(), created.clubId());
        clubService.join(creator.getId(), new JoinCode(created.joinCode()));

        ClubMember restored = clubMemberRepository
                .findByMemberIdAndClubId(creator.getId(), created.clubId()).orElseThrow();
        assertThat(restored.getId()).isEqualTo(membership.getId());
        assertThat(restored.getStatus()).isEqualTo(ClubMemberStatus.JOINED);
        var restoredProgress = readingProgressApi.find(new MemberId(creator.getId()),
                new SpaceId(progressClub.getSpaceId()),
                new ContentId(book.getContentId())).orElseThrow();
        assertThat(restoredProgress.locationId().value()).isEqualTo(passage.getLocationId());
        assertThat(restoredProgress.lastReadAt()).isEqualTo(lastReadAt);
        assertThat(clubMemberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 참여 코드는 거부된다")
    void rejectUnknownJoinCode() {
        assertThatThrownBy(() -> clubService.join(creator.getId(), new JoinCode("NOCODE")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("삭제된 도서가 연결된 기존 모임은 목록과 상세에 DELETED 상태로 보존된다")
    void preservesClubOfDeletedBook() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), book.getId());
        bookRepository.delete(book.getId());

        MyClubsResponse clubs = clubService.findMyClubs(creator.getId());
        ClubDetailResponse detail = clubService.findDetail(creator.getId(), created.clubId());

        assertThat(clubs.clubs().getFirst().book().status()).isEqualTo(Status.DELETED);
        assertThat(detail.book().status()).isEqualTo(Status.DELETED);
    }

    @Test
    @DisplayName("내 모임 목록에 회원 수와 진도가 함께 조회된다")
    void findMyClubsWithProgress() {
        Book smallBook = bookRepository.save(newBook(new BookTitle("작은 책"), null, null, 3, null));
        Chapter chapter = chapterRepository.save(new Chapter(smallBook, new ChapterTitle("1장"), 1));
        Passage second = passageRepository.save(newPassage(chapter, 2, Collections.singletonList(new SentenceContent("본문 2"))));
        ClubCreateResponse first = clubService.create(creator.getId(), new ClubName("1기"), book.getId());
        ClubCreateResponse secondClub = clubService.create(creator.getId(), new ClubName("2기"), smallBook.getId());
        Member joiner = memberRepository.save(new Member(new Nickname("지수")));
        clubService.join(joiner.getId(), new JoinCode(first.joinCode()));
        var progressTarget = clubRepository.findById(secondClub.clubId()).orElseThrow();
        readingProgressApi.update(new MemberId(creator.getId()), new SpaceId(progressTarget.getSpaceId()),
                new ContentId(smallBook.getContentId()),
                new ContentLocationId(second.getLocationId()), LocalDateTime.of(2026, 8, 5, 14, 30));

        MyClubsResponse response = clubService.findMyClubs(creator.getId());

        assertThat(response.clubs()).hasSize(2);
        MyClubResponse firstClub = response.clubs().stream()
                .filter(club -> club.clubId().equals(first.clubId())).findFirst().orElseThrow();
        MyClubResponse progressClub = response.clubs().stream()
                .filter(club -> club.clubId().equals(secondClub.clubId())).findFirst().orElseThrow();
        assertThat(firstClub.memberCount()).isEqualTo(2);
        assertThat(firstClub.myProgress()).isNull();
        assertThat(progressClub.myProgress().lastReadPassageSequence()).isEqualTo(2);
        assertThat(progressClub.myProgress().progressRate()).isEqualTo(67);
    }

    @Test
    @DisplayName("탈퇴한 모임은 내 목록에서 제외되고 참여자 수에도 포함되지 않는다")
    void excludeLeftMembershipFromMyClubsAndCount() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), book.getId());
        Member joiner = memberRepository.save(new Member(new Nickname("지수")));
        clubService.join(joiner.getId(), new JoinCode(created.joinCode()));

        clubService.leave(joiner.getId(), created.clubId());

        MyClubsResponse creatorClubs = clubService.findMyClubs(creator.getId());
        assertThat(creatorClubs.clubs()).singleElement()
                .extracting(MyClubResponse::memberCount)
                .isEqualTo(1L);
        assertThat(clubService.findMyClubs(joiner.getId()).clubs()).isEmpty();
    }

    @Test
    @DisplayName("모임 상세에 참여 코드·참여 순서의 회원 목록·내 진도가 함께 조회된다")
    void findDetailWithMembersAndProgress() {
        Book smallBook = bookRepository.save(newBook(new BookTitle("작은 책"), null, null, 3, null));
        Chapter chapter = chapterRepository.save(new Chapter(smallBook, new ChapterTitle("1장"), 1));
        Passage second = passageRepository.save(newPassage(chapter, 2, Collections.singletonList(new SentenceContent("본문 2"))));
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), smallBook.getId());
        Member joiner = memberRepository.save(new Member(new Nickname("지수")));
        clubService.join(joiner.getId(), new JoinCode(created.joinCode()));
        memberBlockRepository.save(new MemberBlock(creator, joiner));
        var progressClub = clubRepository.findById(created.clubId()).orElseThrow();
        readingProgressApi.update(new MemberId(creator.getId()), new SpaceId(progressClub.getSpaceId()),
                new ContentId(smallBook.getContentId()),
                new ContentLocationId(second.getLocationId()), LocalDateTime.of(2026, 8, 5, 14, 30));

        ClubDetailResponse response = clubService.findDetail(creator.getId(), created.clubId());

        assertThat(response.joinCode()).isEqualTo(created.joinCode());
        assertThat(response.members()).extracting(ClubMemberResponse::nickname).containsExactly("민서", "지수");
        assertThat(response.members()).extracting(ClubMemberResponse::mine, ClubMemberResponse::blocked)
                .containsExactly(tuple(true, false), tuple(false, true));
        assertThat(response.myProgress().lastReadPassageSequence()).isEqualTo(2);
        assertThat(response.myProgress().progressRate()).isEqualTo(67);
    }

    @Test
    @DisplayName("탈퇴 회원은 모임 상세에서 제외되고 자신의 상세 조회도 거부된다")
    void excludeLeftMembershipFromDetail() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), book.getId());
        Member joiner = memberRepository.save(new Member(new Nickname("지수")));
        clubService.join(joiner.getId(), new JoinCode(created.joinCode()));

        clubService.leave(joiner.getId(), created.clubId());

        ClubDetailResponse detail = clubService.findDetail(creator.getId(), created.clubId());
        assertThat(detail.members()).extracting(ClubMemberResponse::nickname).containsExactly("민서");
        assertThatThrownBy(() -> clubService.findDetail(joiner.getId(), created.clubId()))
                .isInstanceOfSatisfying(SpaceQueryPolicyException.class,
                        failure -> assertThat(failure.getCode())
                                .isEqualTo(ErrorCode.SPACE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("읽기 시작 전이면 모임 상세의 내 진도는 null이다")
    void findDetailWithoutProgress() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), book.getId());

        ClubDetailResponse response = clubService.findDetail(creator.getId(), created.clubId());

        assertThat(response.myProgress()).isNull();
    }

    @Test
    @DisplayName("모임에 참여하지 않은 회원의 상세 조회는 거부된다")
    void rejectDetailForOutsider() {
        ClubCreateResponse created = clubService.create(creator.getId(), new ClubName("1기"), book.getId());
        Member outsider = memberRepository.save(new Member(new Nickname("외부인")));

        assertThatThrownBy(() -> clubService.findDetail(outsider.getId(), created.clubId()))
                .isInstanceOfSatisfying(SpaceQueryPolicyException.class,
                        failure -> assertThat(failure.getCode())
                                .isEqualTo(ErrorCode.SPACE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("존재하지 않는 모임의 상세 조회는 거부된다")
    void rejectDetailForUnknownClub() {
        assertThatThrownBy(() -> clubService.findDetail(creator.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }
}
