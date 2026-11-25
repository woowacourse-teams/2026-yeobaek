package yeobaek.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.event.ClubMemberJoinedEvent;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubMemberCount;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

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
    private MemberBlockRepository memberBlockRepository;
    @Mock
    private BookCoverUrlResolver bookCoverUrlResolver;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ClubMemberCount memberCount;
    @InjectMocks
    private ClubService clubService;

    private Book book;
    private Club club;
    private Member reader;
    private ClubMember membership;

    @BeforeEach
    void setUp() {
        book = new Book(new BookTitle("함께 읽는 책"), null, null, 4, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 20L);
        club = new Club(new ClubName("독서 모임"), book, new JoinCode("CODE01"));
        ReflectionTestUtils.setField(club, "id", 10L);
        reader = member(1L, "독자");
        membership = new ClubMember(reader, club);
    }

    @Test
    @DisplayName("모임 생성자는 발급된 코드의 모임에 자동 참여한다")
    void creatorJoinsNewClub() {
        given(bookRepository.getById(20L)).willReturn(book);
        given(clubRepository.save(any(Club.class))).willAnswer(invocation -> {
            Club created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", 11L);
            return created;
        });
        given(memberRepository.getReferenceById(1L)).willReturn(reader);

        var response = clubService.create(1L, new ClubName("새 모임"), 20L);

        ArgumentCaptor<ClubMember> saved = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getMember()).isSameAs(reader);
        assertThat(saved.getValue().getClub().getId()).isEqualTo(response.clubId());
        assertThat(saved.getValue().isJoined()).isTrue();
        assertThat(response.joinCode()).matches("[A-Z0-9]{6}");
    }

    @Test
    @DisplayName("유효한 코드로 최초 참여하면 참여 상태를 저장한다")
    void joinByCode() {
        given(clubRepository.findByJoinCode("CODE01")).willReturn(Optional.of(club));
        given(memberRepository.getReferenceById(1L)).willReturn(reader);

        var response = clubService.join(1L, new JoinCode("CODE01"));

        ArgumentCaptor<ClubMember> saved = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getMember()).isSameAs(reader);
        assertThat(saved.getValue().getClub()).isSameAs(club);
        assertThat(response.clubId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이미 참여한 모임의 재참여는 기존 상태와 응답을 유지한다")
    void repeatedJoin() {
        givenMembership();

        var response = clubService.join(1L, new JoinCode("CODE01"));

        assertThat(response.clubId()).isEqualTo(10L);
        assertThat(membership.isJoined()).isTrue();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("탈퇴 후 재참여하면 기존 진도를 보존하고 참여 알림 대상을 만든다")
    void rejoinPreservesReading() {
        Passage passage = new Passage(new Chapter(book, new ChapterTitle("첫 장"), 1), 2,
                List.of(new SentenceContent("저장한 본문")));
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        membership.updateProgress(passage, readAt);
        membership.leave();
        givenMembership();

        clubService.join(1L, new JoinCode("CODE01"));

        assertThat(membership.isJoined()).isTrue();
        assertThat(membership.getLastReadPassage()).isSameAs(passage);
        assertThat(membership.getLastReadAt()).isEqualTo(readAt);
        ArgumentCaptor<ClubMemberJoinedEvent> event = ArgumentCaptor.forClass(ClubMemberJoinedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().memberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제된 도서의 모임에는 참여할 수 없다")
    void rejectJoiningClubOfDeletedBook() {
        book.delete();
        given(clubRepository.findByJoinCode("CODE01")).willReturn(Optional.of(club));

        assertThatThrownBy(() -> clubService.join(1L, new JoinCode("CODE01")))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("없는 참여 코드는 참여 코드 오류로 구분한다")
    void missingJoinCode() {
        assertThatThrownBy(() -> clubService.join(1L, new JoinCode("CODE01")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.JOIN_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("탈퇴를 반복해도 탈퇴 상태를 유지한다")
    void repeatedLeave() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(clubMemberRepository.findByMemberIdAndClubId(1L, 10L)).willReturn(Optional.of(membership));

        clubService.leave(1L, 10L);
        clubService.leave(1L, 10L);

        assertThat(membership.isJoined()).isFalse();
    }

    @Test
    @DisplayName("가입 이력이 없으면 탈퇴할 수 없다")
    void rejectLeaveWithoutMembership() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));

        assertThatThrownBy(() -> clubService.leave(1L, 10L))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
    }

    @Test
    @DisplayName("없는 모임을 조회하거나 탈퇴하면 모임 오류로 구분한다")
    void missingClub() {
        assertThatThrownBy(() -> clubService.findDetail(1L, 10L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CLUB_NOT_FOUND));
        assertThatThrownBy(() -> clubService.leave(1L, 10L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CLUB_NOT_FOUND));
    }

    @Test
    @DisplayName("상세는 내 표시와 차단 여부를 회원별로 제공하고 삭제 도서 정보도 보존한다")
    void detailMemberVisibility() {
        ClubMember blocked = new ClubMember(member(2L, "다른 독자"), club);
        book.delete();
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(clubMemberRepository.findAllJoinedWithMemberByClubId(10L)).willReturn(List.of(membership, blocked));
        given(memberBlockRepository.findBlockedMemberIds(1L, List.of(1L, 2L))).willReturn(List.of(2L));

        var response = clubService.findDetail(1L, 10L);

        assertThat(response.members()).extracting(member -> tuple(member.memberId(), member.mine(), member.blocked()))
                .containsExactly(tuple(1L, true, false), tuple(2L, false, true));
        assertThat(response.myProgress()).isNull();
        assertThat(response.book().status()).isEqualTo(BookStatus.DELETED);
    }

    @Test
    @DisplayName("참여 중인 회원 목록에 없으면 모임 상세를 볼 수 없다")
    void rejectDetailWithoutMembership() {
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));

        assertThatThrownBy(() -> clubService.findDetail(1L, 10L))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER));
    }

    @Test
    @DisplayName("내 모임은 집계된 인원과 저장 진도 및 삭제 도서 상태를 반환한다")
    void myClubsWithProgress() {
        Passage passage = new Passage(new Chapter(book, new ChapterTitle("첫 장"), 1), 2,
                List.of(new SentenceContent("읽은 본문")));
        membership.updateProgress(passage, LocalDateTime.of(2026, 1, 1, 12, 0));
        book.delete();
        given(clubMemberRepository.findAllJoinedWithClubAndBookByMemberId(1L)).willReturn(List.of(membership));
        given(clubMemberRepository.countJoinedMembersByClubIds(List.of(10L))).willReturn(List.of(memberCount));
        given(memberCount.getClubId()).willReturn(10L);
        given(memberCount.getMemberCount()).willReturn(3L);

        var response = clubService.findMyClubs(1L);

        assertThat(response.clubs()).singleElement().satisfies(result -> {
            assertThat(result.memberCount()).isEqualTo(3L);
            assertThat(result.myProgress().lastReadPassageSequence()).isEqualTo(2);
            assertThat(result.myProgress().progressRate()).isEqualTo(50);
            assertThat(result.book().status()).isEqualTo(BookStatus.DELETED);
        });
    }

    private void givenMembership() {
        given(clubRepository.findByJoinCode("CODE01")).willReturn(Optional.of(club));
        given(clubMemberRepository.findByMemberIdAndClubId(1L, 10L)).willReturn(Optional.of(membership));
    }

    private Member member(Long id, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
