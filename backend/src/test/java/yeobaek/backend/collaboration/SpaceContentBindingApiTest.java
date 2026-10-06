package yeobaek.backend.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.domain.Chapter;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.space.club.persistence.Club;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.space.publicroom.persistence.PublicRoom;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.readmodel.book.SpaceBookReadModel;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.web.v1.ClubService;

class SpaceContentBindingApiTest extends IntegrationTest {

    @Autowired
    private SpaceContentBindingApi bindingApi;
    @Autowired
    private ContentApi contentApi;
    @Autowired
    private ContentBodyApi bodyApi;
    @Autowired
    private BookManagementRepository bookRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private PassageRepository passageRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private ClubMemberRepository membershipRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PublicRoomRepository roomRepository;
    @Autowired
    private ReadingProgressApi readingApi;
    @Autowired
    private SpaceBookReadModel bookReadModel;
    @Autowired
    private ClubService clubService;

    @Test
    @DisplayName("모임의 도서에 속한 본문이면 canonical 콘텐츠 소속으로 판단한다")
    void isReadingOwnBookPassage() {
        Book book = book(1);
        Passage passage = passage(book, 1);
        Club club = club(book, "OWN001");
        ContentId contentId = bindingApi.findContents(spaceId(club)).getFirst();
        assertThat(contentApi.ownsLocation(contentId, new ContentLocationId(passage.getLocationId()))).isTrue();
    }

    @Test
    @DisplayName("다른 도서의 본문은 공간에 연결된 콘텐츠 소속이 아니다")
    void isNotReadingOtherBookPassage() {
        Book book = book(1);
        Passage foreign = passage(book(1), 1);
        Club club = club(book, "OTHER1");
        ContentId contentId = bindingApi.findContents(spaceId(club)).getFirst();
        assertThat(contentApi.ownsLocation(contentId, new ContentLocationId(foreign.getLocationId()))).isFalse();
    }

    @Test
    @DisplayName("본문 개수는 저장된 도서 메타데이터를 사용한다")
    void totalPassageCountDelegatesToBook() {
        Book book = book(7);
        passage(book, 1);
        Club club = club(book, "TOTAL1");
        assertThat(bodyApi.passageCount(bindingApi.findContents(spaceId(club)).getFirst())).isEqualTo(7);
    }

    @Test
    @DisplayName("같은 도서를 읽는 모임은 같은 콘텐츠 식별자를 공유한다")
    void sharesContentIdentityAcrossClubs() {
        Book first = book(1);
        Book second = book(1);
        List<Club> clubs = List.of(club(first, "BOOK01"), club(first, "BOOK02"), club(second, "BOOK03"));
        assertThat(bindingApi.findContents(spaceId(clubs.get(0))))
                .containsExactly(new ContentId(first.getContentId()));
        assertThat(bindingApi.findContents(spaceId(clubs.get(1))))
                .containsExactly(new ContentId(first.getContentId()));
        assertThat(bindingApi.findContents(spaceId(clubs.get(2))))
                .containsExactly(new ContentId(second.getContentId()));
    }

    @Test
    @DisplayName("모임 진도율은 최근 본문 순서와 연결된 도서의 전체 본문 개수로 계산한다")
    void progressRateDelegatesTotalPassageCountToClub() {
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        Book book = book(4);
        Passage passage = passage(book, 3);
        Club club = club(book, "RATE01");
        membershipRepository.save(new ClubMember(new MemberId(reader.getId()), club));
        readingApi.update(new MemberId(reader.getId()), spaceId(club), new ContentId(book.getContentId()),
                new ContentLocationId(passage.getLocationId()), LocalDateTime.of(2026, 10, 2, 12, 0));

        assertThat(clubService.findDetail(reader.getId(), club.getId()).myProgress().progressRate()).isEqualTo(75);
    }

    @Test
    @DisplayName("공개방의 유일한 콘텐츠 제약 실패는 generic 연결까지 함께 롤백한다")
    void directBindingFailureRollsBackGenericRow() {
        Book book = book(1);
        PublicRoom first = roomRepository.save(newPublicRoom());
        PublicRoom second = roomRepository.save(newPublicRoom());
        ContentId contentId = new ContentId(book.getContentId());
        SpaceId firstId = new SpaceId(first.getSpaceId());
        SpaceId secondId = new SpaceId(second.getSpaceId());
        bindingApi.bind(firstId, contentId);

        assertThatThrownBy(() -> bindingApi.bind(secondId, contentId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(bindingApi.isBound(firstId, contentId)).isTrue();
        assertThat(bindingApi.findContents(secondId)).isEmpty();
    }

    @Test
    @DisplayName("공간의 도서 메타데이터는 canonical 연결에서 조회한다")
    void canonicalBindingProvidesBookMetadata() {
        Book book = book(8);
        Club club = club(book, "META01");
        var metadata = bookReadModel.findBook(spaceId(club)).orElseThrow();
        assertThat(metadata.bookId()).isEqualTo(book.getId());
        assertThat(metadata.contentId()).isEqualTo(new ContentId(book.getContentId()));
        assertThat(metadata.passageCount()).isEqualTo(8);
    }

    private Book book(int passageCount) {
        return bookRepository.save(newBook(new BookTitle("도서"), null, null, passageCount, null));
    }

    private Passage passage(Book book, int sequence) {
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("목차"), 1));
        return passageRepository.save(newPassage(chapter, sequence, List.of(new SentenceContent("본문"))));
    }

    private Club club(Book book, String code) {
        Club club = clubRepository.save(newClub(new ClubName("모임"), new JoinCode(code)));
        bindingApi.bind(spaceId(club), new ContentId(book.getContentId()));
        return club;
    }

    private SpaceId spaceId(Club club) {
        return new SpaceId(club.getSpaceId());
    }
}
