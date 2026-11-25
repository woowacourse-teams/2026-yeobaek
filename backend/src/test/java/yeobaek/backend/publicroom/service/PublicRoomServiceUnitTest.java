package yeobaek.backend.publicroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.domain.PublicRoomMember;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.publicroom.repository.PublicRoomVisitorCount;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class PublicRoomServiceUnitTest {

    @Mock
    private PublicRoomRepository publicRoomRepository;
    @Mock
    private PublicRoomMemberRepository publicRoomMemberRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AuthorBookRepository authorBookRepository;
    @Mock
    private BookCoverUrlResolver bookCoverUrlResolver;
    @Mock
    private PublicRoomVisitorCount visitorCount;
    @InjectMocks
    private PublicRoomService publicRoomService;

    private Book book;
    private PublicRoom room;
    private PublicRoomMember membership;
    private Passage passage;

    @BeforeEach
    void setUp() {
        book = new Book(new BookTitle("공개방 도서"), null, null, 4, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 20L);
        room = new PublicRoom(book);
        ReflectionTestUtils.setField(room, "id", 10L);
        membership = new PublicRoomMember(new Member(new Nickname("독자")), room);
        passage = new Passage(new Chapter(book, new ChapterTitle("첫 장"), 1), 2,
                List.of(new SentenceContent("본문")));
    }

    @Test
    @DisplayName("목록은 방문 수 내림차순으로 정렬하고 미방문 방의 진도는 비운다")
    void sortByVisitors() {
        PublicRoom other = new PublicRoom(book);
        ReflectionTestUtils.setField(other, "id", 11L);
        given(publicRoomRepository.findAllActiveWithBook()).willReturn(List.of(room, other));
        given(publicRoomMemberRepository.countVisitorsByPublicRoomIds(List.of(10L, 11L)))
                .willReturn(List.of(visitorCount));
        given(visitorCount.getPublicRoomId()).willReturn(11L);
        given(visitorCount.getVisitorCount()).willReturn(3L);

        var response = publicRoomService.findAll(1L, PublicRoomSort.MOST_VISITED, null);

        assertThat(response.publicRooms()).extracting(result -> result.publicRoomId()).containsExactly(11L, 10L);
        assertThat(response.publicRooms()).allSatisfy(result -> assertThat(result.myProgress()).isNull());
    }

    @Test
    @DisplayName("미방문 공개방도 가입 없이 상세를 조회한다")
    void detailBeforeVisit() {
        given(publicRoomRepository.findWithBookById(10L)).willReturn(Optional.of(room));

        var response = publicRoomService.findDetail(1L, 10L);

        assertThat(response.publicRoomId()).isEqualTo(10L);
        assertThat(response.myProgress()).isNull();
        assertThat(response.lastVisitedAt()).isNull();
    }

    @Test
    @DisplayName("방문 시각을 갱신해도 마지막 읽기 위치는 유지한다")
    void visitPreservesProgress() {
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        membership.updateProgress(passage, readAt);
        givenMembership();
        LocalDateTime before = LocalDateTime.now();

        publicRoomService.visit(1L, 10L);

        assertThat(membership.getLastVisitedAt()).isBetween(before, LocalDateTime.now());
        assertThat(membership.getLastReadPassage()).isSameAs(passage);
        assertThat(membership.getLastReadAt()).isEqualTo(readAt);
    }

    @Test
    @DisplayName("공개방의 앞부분을 다시 읽으면 진도는 후퇴하고 방문 시각은 바뀌지 않는다")
    void progressPreservesVisit() {
        LocalDateTime visitedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        membership.visit(visitedAt);
        membership.updateProgress(new Passage(passage.getChapter(), 4, List.of(new SentenceContent("마지막"))), visitedAt);
        givenMembership();
        given(passageRepository.findById(30L)).willReturn(Optional.of(passage));

        var response = publicRoomService.updateProgress(1L, 10L, 30L);

        assertThat(response.lastReadPassageSequence()).isEqualTo(2);
        assertThat(response.progressRate()).isEqualTo(50);
        assertThat(membership.getLastVisitedAt()).isEqualTo(visitedAt);
    }

    @Test
    @DisplayName("다른 도서 본문을 공개방 진도로 저장할 수 없다")
    void rejectOtherBook() {
        Book other = new Book(new BookTitle("다른 책"), null, null, 1, null, null, null, null, null);
        ReflectionTestUtils.setField(other, "id", 21L);
        Passage otherPassage = new Passage(new Chapter(other, new ChapterTitle("첫 장"), 1), 1,
                List.of(new SentenceContent("다른 본문")));
        given(publicRoomRepository.findWithBookByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(passageRepository.findById(30L)).willReturn(Optional.of(otherPassage));

        assertThatThrownBy(() -> publicRoomService.updateProgress(1L, 10L, 30L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.PASSAGE_NOT_FOUND));
        assertThat(membership.hasProgress()).isFalse();
    }

    @Test
    @DisplayName("삭제 도서는 공개방을 방문해도 활동을 갱신할 수 없다")
    void rejectDeletedBookVisit() {
        book.delete();
        given(publicRoomRepository.findWithBookByIdForUpdate(10L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> publicRoomService.visit(1L, 10L))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
        assertThat(membership.getLastVisitedAt()).isNull();
    }

    @Test
    @DisplayName("없는 공개방의 상세는 공개방 오류로 구분한다")
    void missingRoom() {
        assertThatThrownBy(() -> publicRoomService.findDetail(1L, 10L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.PUBLIC_ROOM_NOT_FOUND));
    }

    private void givenMembership() {
        given(publicRoomRepository.findWithBookByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(publicRoomMemberRepository.findByMemberIdAndPublicRoomId(1L, 10L))
                .willReturn(Optional.of(membership));
    }
}
