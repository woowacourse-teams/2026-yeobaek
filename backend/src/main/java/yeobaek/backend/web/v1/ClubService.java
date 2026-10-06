package yeobaek.backend.web.v1;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.club.ClubCreationWorkflow;
import yeobaek.backend.application.club.ClubMembershipWorkflow;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryService;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.content.api.idmapping.ContentIdMappingNotFoundException;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.space.api.club.ClubResponse;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.ForbiddenException;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.web.club.dto.ClubCreateResponse;
import yeobaek.backend.web.club.dto.ClubDetailResponse;
import yeobaek.backend.web.club.dto.ClubJoinResponse;
import yeobaek.backend.web.club.dto.ClubMemberResponse;
import yeobaek.backend.web.club.dto.MyClubResponse;
import yeobaek.backend.web.club.dto.MyClubsResponse;
import yeobaek.backend.web.club.dto.MyProgressResponse;
import yeobaek.backend.web.common.dto.BookResponse;
import yeobaek.backend.web.common.dto.BookResponse.Status;

@Service("clubCompatibilityService")
@RequiredArgsConstructor
public class ClubService {

    private final ClubApi clubs;
    private final ClubMembershipApi membershipApi;
    private final ClubMembershipWorkflow memberships;
    private final ClubCreationWorkflow creations;
    private final SpaceQueryService queries;
    private final ContentIdMappingApi idMappings;

    @Transactional
    public ClubCreateResponse create(Long memberId, ClubName name, Long bookId) {
        var created = creations.create(new MemberId(memberId), name, resolveBook(bookId));
        ClubResponse club = created.club();
        return new ClubCreateResponse(club.clubId(), club.name(), club.joinCode(), book(created.content()));
    }

    @Transactional
    public ClubJoinResponse join(Long memberId, JoinCode joinCode) {
        ClubResponse club = clubs.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_CODE_NOT_FOUND,
                        "참여 코드에 해당하는 모임이 존재하지 않습니다.",
                        Map.of("lookup", "joinCode")));
        var joined = memberships.join(new MemberId(memberId), club.id());
        return new ClubJoinResponse(club.clubId(), club.name(), book(joined.content()));
    }

    @Transactional
    public void leave(Long memberId, Long clubId) {
        ClubResponse club = requireClub(clubId);
        if (!membershipApi.hasMembershipHistory(new MemberId(memberId), clubId)) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER,
                    "가입 이력이 있는 회원만 모임을 탈퇴할 수 있습니다: clubId=" + clubId,
                    Map.of("clubId", clubId.toString()));
        }
        memberships.leave(new MemberId(memberId), club.id());
    }

    @Transactional(readOnly = true)
    public MyClubsResponse findMyClubs(Long memberId) {
        return new MyClubsResponse(queries.findMine(new MemberId(memberId), SpaceKind.CLUB).stream()
                .map(this::clubSummary).toList());
    }

    @Transactional(readOnly = true)
    public ClubDetailResponse findDetail(Long memberId, Long clubId) {
        ClubResponse club = requireClub(clubId);
        var result = queries.findDetail(new MemberId(memberId), club.id());
        var data = (SpaceQueryResult.ClubDetail) result.data();
        return new ClubDetailResponse(clubId, data.name(), data.joinCode(), book(data.content()),
                progress(data.myProgress()), data.members().stream().map(member -> new ClubMemberResponse(
                        member.memberId().value(), member.nickname(), member.mine(), member.blocked())).toList());
    }

    private MyClubResponse clubSummary(SpaceQueryResult result) {
        ClubResponse club = clubs.findBySpaceId(result.spaceId()).orElseThrow();
        var data = (SpaceQueryResult.ClubSummary) result.data();
        return new MyClubResponse(club.clubId(), data.name(), data.memberCount(), book(data.content()),
                progress(data.myProgress()));
    }

    private MyProgressResponse progress(SpaceQueryResult.Progress progress) {
        if (progress == null) {
            return null;
        }
        return new MyProgressResponse(progress.lastReadPassageSequence(), progress.progressRate(),
                progress.lastReadAt());
    }

    private BookResponse book(ContentCardResult content) {
        Long bookId = idMappings.toImplementationIds(ContentKind.BOOK, List.of(content.contentId()))
                .get(content.contentId());
        return new BookResponse(bookId, content.title(), content.creators(), content.coverImageUrl(),
                content.unitCount(), content.available() ? Status.ACTIVE : Status.DELETED);
    }

    private yeobaek.backend.shared.identity.ContentId resolveBook(Long bookId) {
        try {
            return idMappings.toContentId(ContentKind.BOOK, bookId);
        } catch (ContentIdMappingNotFoundException failure) {
            throw new NotFoundException(ErrorCode.BOOK_NOT_FOUND, "도서가 존재하지 않습니다: bookId=" + bookId,
                    Map.of("bookId", bookId.toString()), failure);
        }
    }

    private ClubResponse requireClub(Long clubId) {
        return clubs.findById(clubId).orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND,
                "모임이 존재하지 않습니다: clubId=" + clubId,
                Map.of("clubId", clubId.toString())));
    }
}
