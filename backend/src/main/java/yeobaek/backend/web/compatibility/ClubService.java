package yeobaek.backend.web.compatibility;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.club.ClubCreationWorkflow;
import yeobaek.backend.application.club.ClubMembershipWorkflow;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.ClubMembers;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.domain.vo.ProgressRate;
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
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentBodyApi;
import yeobaek.backend.content.api.ContentUnavailableFailure;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberBlockApi;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.readmodel.book.SpaceBookReadModel;
import yeobaek.backend.readmodel.book.SpaceBookReadModel.BookSnapshot;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubMembershipWorkflow membershipWorkflow;
    private final ClubCreationWorkflow creationWorkflow;
    private final SpaceBookReadModel bookReadModel;
    private final ContentApi contentApi;
    private final ContentBodyApi contentBodyApi;
    private final MemberQuery memberQuery;
    private final MemberBlockApi memberBlockApi;
    private final BookCoverUrlResolver bookCoverUrlResolver;
    private final ReadingProgressApi readingProgressApi;

    @Transactional
    public ClubCreateResponse create(Long memberId, ClubName name, Long bookId) {
        logAttempt("club.create", null, bookId);
        BookSnapshot book = bookReadModel.findByBookId(bookId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND,
                        "도서가 존재하지 않습니다: bookId=" + bookId, Map.of(BOOK_ID, bookId.toString())));
        requireAvailable(book);
        var club = creationWorkflow.create(new MemberId(memberId), name, book.contentId());
        var response = new ClubCreateResponse(club.clubId(), club.name(), club.joinCode(), toBookResponse(book));
        logSuccess("club.create", club.clubId(), bookId);
        return response;
    }

    @Transactional
    public ClubJoinResponse join(Long memberId, JoinCode joinCode) {
        logAttempt("club.join", null, null);
        Club club = clubRepository.findByJoinCode(joinCode.value())
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_CODE_NOT_FOUND,
                        "참여 코드에 해당하는 모임이 존재하지 않습니다."));
        BookSnapshot book = requireBook(club);
        requireAvailable(book);
        membershipWorkflow.join(new MemberId(memberId), new SpaceId(club.getSpaceId()));
        var response = new ClubJoinResponse(club.getId(), club.getName(), toBookResponse(book));
        logSuccess("club.join", club.getId(), book.bookId());
        return response;
    }

    @Transactional
    public void leave(Long memberId, Long clubId) {
        logAttempt("club.leave", clubId, null);
        Club club = findClub(clubId, "탈퇴할");
        clubMemberRepository.findByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER,
                        "가입 이력이 있는 회원만 모임을 탈퇴할 수 있습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        membershipWorkflow.leave(new MemberId(memberId), new SpaceId(club.getSpaceId()));
        logSuccess("club.leave", clubId, null);
    }

    @Transactional(readOnly = true)
    public MyClubsResponse findMyClubs(Long memberId) {
        logAttempt("club.findMyClubs", null, null);
        ClubMembers memberships = new ClubMembers(clubMemberRepository.findAllJoinedWithClubByMemberId(memberId));
        Map<Long, Long> memberCounts = clubMemberRepository.countJoinedMembersByClubIds(memberships.clubIds()).stream()
                .collect(Collectors.toMap(ClubMemberCount::getClubId, ClubMemberCount::getMemberCount));
        var response = new MyClubsResponse(memberships.asList().stream().map(membership -> {
            Club club = membership.getClub();
            BookSnapshot book = requireBook(club);
            return new MyClubResponse(club.getId(), club.getName(), memberCounts.getOrDefault(club.getId(), 0L),
                    toBookResponse(book), toMyProgress(membership, book));
        }).toList());
        log.atInfo().addKeyValue(OPERATION, "club.findMyClubs").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", response.clubs().size()).log("내 모임 목록을 조회했습니다.");
        return response;
    }

    @Transactional(readOnly = true)
    public ClubDetailResponse findDetail(Long memberId, Long clubId) {
        logAttempt("club.findDetail", clubId, null);
        Club club = findClub(clubId, "상세 정보를 조회할");
        ClubMembers memberships = new ClubMembers(clubMemberRepository.findAllJoinedByClubId(clubId));
        ClubMember mine = memberships.findByMemberId(memberId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 상세 정보를 조회할 수 있습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        Set<MemberId> memberIds = memberships.memberIds().stream().map(MemberId::new).collect(Collectors.toSet());
        Set<MemberId> blocked = memberBlockApi.findBlockedMemberIds(new MemberId(memberId), memberIds);
        var profiles = memberQuery.findProfiles(memberIds);
        BookSnapshot book = requireBook(club);
        var response = new ClubDetailResponse(club.getId(), club.getName(), club.getJoinCode(), toBookResponse(book),
                toMyProgress(mine, book), memberships.asList().stream().map(membership -> {
                    MemberId actor = new MemberId(membership.getMemberId());
                    var profile = profiles.get(actor);
                    return new ClubMemberResponse(actor.value(), profile.nickname(), membership.isOwnedBy(memberId),
                            !membership.isOwnedBy(memberId) && blocked.contains(actor));
                }).toList());
        log.atInfo().addKeyValue(OPERATION, "club.findDetail").addKeyValue(RESULT, SUCCESS).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("memberCount", response.members().size()).log("모임 상세를 조회했습니다.");
        return response;
    }

    private Club findClub(Long clubId, String purpose) {
        return clubRepository.findById(clubId).orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND,
                purpose + " 모임이 존재하지 않습니다: clubId=" + clubId, Map.of(CLUB_ID, clubId.toString())));
    }

    private BookSnapshot requireBook(Club club) {
        return bookReadModel.findBook(new SpaceId(club.getSpaceId()))
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND,
                        "모임에서 읽는 도서가 존재하지 않습니다: clubId=" + club.getId()));
    }

    private void requireAvailable(BookSnapshot book) {
        try {
            contentApi.requireAvailable(book.contentId());
        } catch (ContentUnavailableFailure failure) {
            throw new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE,
                    "더 이상 이용할 수 없는 도서입니다.", failure);
        }
    }

    private MyProgressResponse toMyProgress(ClubMember membership, BookSnapshot book) {
        return readingProgressApi.find(new MemberId(membership.getMemberId()),
                        new SpaceId(membership.getClub().getSpaceId()), book.contentId())
                .map(progress -> {
                    var passage = contentBodyApi.findPassageByLocation(book.contentId(), progress.locationId())
                            .orElseThrow();
                    return new MyProgressResponse(passage.sequence(),
                            ProgressRate.calculate(passage.sequence(), book.passageCount()).roundedPercentage(),
                            progress.lastReadAt());
                }).orElse(null);
    }

    private ClubBookResponse toBookResponse(BookSnapshot book) {
        return new ClubBookResponse(book.bookId(), book.title(), book.authors(),
                bookCoverUrlResolver.resolve(book.coverImageKey()), book.passageCount(), BookStatus.valueOf(book.status()));
    }

    private void logAttempt(String operation, Long clubId, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(CLUB_ID, clubId).addKeyValue(BOOK_ID, bookId)
                .log("모임 작업을 시작합니다.");
    }

    private void logSuccess(String operation, Long clubId, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(BOOK_ID, bookId).log("모임 작업을 완료했습니다.");
    }
}
