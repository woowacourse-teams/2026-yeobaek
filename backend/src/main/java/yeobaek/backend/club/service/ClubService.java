package yeobaek.backend.club.service;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.RECOVERED;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.ClubMembers;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.ClubCreateResponse;
import yeobaek.backend.club.dto.ClubDetailResponse;
import yeobaek.backend.club.dto.ClubJoinResponse;
import yeobaek.backend.club.dto.ClubMemberResponse;
import yeobaek.backend.club.dto.MyClubResponse;
import yeobaek.backend.club.dto.MyClubsResponse;
import yeobaek.backend.club.dto.MyProgressResponse;
import yeobaek.backend.club.repository.ClubMemberCount;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubService {

    private static final int MAX_JOIN_CODE_ATTEMPTS = 5;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ActiveBookRepository bookRepository;
    private final AuthorBookRepository authorBookRepository;
    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional
    public ClubCreateResponse create(Long memberId, ClubName name, Long bookId) {
        logAttempt("club.create", null, bookId);
        Book book = bookRepository.getById(bookId);
        Club club = clubRepository.save(new Club(name, book, generateUniqueJoinCode()));
        clubMemberRepository.save(new ClubMember(memberRepository.getReferenceById(memberId), club));
        var response = new ClubCreateResponse(club.getId(), club.getName(), club.getJoinCode(),
                toBookResponse(book, authorNames(book)));
        logSuccess("club.create", club.getId(), bookId);
        return response;
    }

    @Transactional
    public ClubJoinResponse join(Long memberId, JoinCode joinCode) {
        logAttempt("club.join", null, null);
        Club club = clubRepository.findByJoinCode(joinCode.value())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.JOIN_CODE_NOT_FOUND,
                        "참여 코드에 해당하는 모임이 존재하지 않습니다."));
        club.ensureBookAvailable();
        clubMemberRepository.findByMemberIdAndClubId(memberId, club.getId())
                .ifPresentOrElse(ClubMember::rejoin,
                        () -> clubMemberRepository.save(
                                new ClubMember(memberRepository.getReferenceById(memberId), club)));
        Book book = club.getBook();
        var response = new ClubJoinResponse(club.getId(), club.getName(), toBookResponse(book, authorNames(book)));
        logSuccess("club.join", club.getId(), book.getId());
        return response;
    }

    @Transactional
    public void leave(Long memberId, Long clubId) {
        logAttempt("club.leave", clubId, null);
        clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "탈퇴할 모임이 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        ClubMember clubMember = clubMemberRepository.findByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "가입 이력이 있는 회원만 모임을 탈퇴할 수 있습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        clubMember.leave();
        logSuccess("club.leave", clubId, null);
    }

    @Transactional(readOnly = true)
    public MyClubsResponse findMyClubs(Long memberId) {
        logAttempt("club.findMyClubs", null, null);
        ClubMembers myClubMemberships = new ClubMembers(
                clubMemberRepository.findAllJoinedWithClubAndBookByMemberId(memberId));
        Map<Long, Long> memberCounts = clubMemberRepository
                .countJoinedMembersByClubIds(myClubMemberships.clubIds()).stream()
                .collect(Collectors.toMap(ClubMemberCount::getClubId, ClubMemberCount::getMemberCount));
        Map<Long, List<String>> authorNames = authorNamesByBookId(myClubMemberships.bookIds());
        var response = new MyClubsResponse(myClubMemberships.asList().stream()
                .map(clubMember -> {
                    Club club = clubMember.getClub();
                    Book book = club.getBook();
                    return new MyClubResponse(club.getId(), club.getName(),
                            memberCounts.getOrDefault(club.getId(), 0L),
                            toBookResponse(book, authorNames.getOrDefault(book.getId(), List.of())),
                            toMyProgress(clubMember));
                })
                .toList());
        log.atInfo().addKeyValue(OPERATION, "club.findMyClubs").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", response.clubs().size())
                .log("내 모임 목록을 조회했습니다.");
        return response;
    }

    @Transactional(readOnly = true)
    public ClubDetailResponse findDetail(Long memberId, Long clubId) {
        logAttempt("club.findDetail", clubId, null);
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "상세 정보를 조회할 모임이 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        ClubMembers clubMembers = new ClubMembers(
                clubMemberRepository.findAllJoinedWithMemberByClubId(clubId));
        ClubMember myMembership = clubMembers.findByMemberId(memberId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 상세 정보를 조회할 수 있습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        Set<Long> blockedMemberIds = blockedMemberIds(memberId, clubMembers.memberIds());
        Book book = club.getBook();
        var response = new ClubDetailResponse(club.getId(), club.getName(), club.getJoinCode(),
                toBookResponse(book, authorNames(book)),
                toMyProgress(myMembership),
                clubMembers.asList().stream()
                        .map(clubMember -> new ClubMemberResponse(clubMember.getMember().getId(),
                                clubMember.getMember().getNickname(), clubMember.isOwnedBy(memberId),
                                !clubMember.isOwnedBy(memberId)
                                        && blockedMemberIds.contains(clubMember.getMember().getId())))
                        .toList());
        log.atInfo().addKeyValue(OPERATION, "club.findDetail").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("memberCount", response.members().size()).log("모임 상세를 조회했습니다.");
        return response;
    }

    private Set<Long> blockedMemberIds(Long memberId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(memberBlockRepository.findBlockedMemberIds(memberId, memberIds));
    }

    private MyProgressResponse toMyProgress(ClubMember clubMember) {
        if (clubMember.getLastReadPassage() == null) {
            return null;
        }
        int sequence = clubMember.getLastReadPassage().getSequence().value();
        return new MyProgressResponse(sequence, clubMember.progressRate(), clubMember.getLastReadAt());
    }

    private JoinCode generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            JoinCode code = JoinCode.generate();
            if (!clubRepository.existsByJoinCode(code.value())) {
                if (attempt > 0) {
                    log.atWarn().addKeyValue(OPERATION, "club.generateUniqueJoinCode").addKeyValue(RESULT, RECOVERED)
                            .addKeyValue("retryCount", attempt).log("참여 코드 충돌을 재시도해 복구했습니다.");
                }
                return code;
            }
        }
        throw new IllegalStateException("참여 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private List<String> authorNames(Book book) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getName().value())
                .collect(Collectors.toList());
    }

    private Map<Long, List<String>> authorNamesByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getName().value(), Collectors.toList())));
    }

    private ClubBookResponse toBookResponse(Book book, List<String> authors) {
        return ClubBookResponse.of(book, authors, bookCoverUrlResolver.resolve(book.getCoverImageKey()));
    }

    private void logAttempt(String operation, Long clubId, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(BOOK_ID, bookId)
                .log("모임 작업을 시작합니다.");
    }

    private void logSuccess(String operation, Long clubId, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(BOOK_ID, bookId)
                .log("모임 작업을 완료했습니다.");
    }
}
