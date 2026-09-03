package yeobaek.backend.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.member.dto.BlockedMembersResponse;
import yeobaek.backend.member.dto.MemberCreateRequest;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.service.MemberBlockService;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@Tag(name = "회원")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private static final String MEMBER_ID_SECURITY_SCHEME = "memberId";

    private final MemberService memberService;
    private final MemberBlockService memberBlockService;
    private final AnalyticsTracker analyticsTracker;

    @Operation(summary = "회원 생성", description = "닉네임 입력만으로 회원을 생성하고 ID를 발급한다. 헤더 불필요(최초 진입).")
    @PostMapping("/api/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberCreateResponse createMember(@RequestBody MemberCreateRequest request) {
        MemberCreateResponse response = memberService.create(request.nickname());
        analyticsTracker.track(response.memberId(), AnalyticsEvent.memberCreated());
        return response;
    }

    @Operation(summary = "차단 목록 조회", description = "요청 회원이 차단한 회원을 닉네임과 회원 ID 오름차순으로 반환한다.")
    @SecurityRequirement(name = MEMBER_ID_SECURITY_SCHEME)
    @GetMapping("/api/members/me/blocks")
    public BlockedMembersResponse findBlockedMembers(@AuthMember Long memberId) {
        return memberBlockService.findBlockedMembers(memberId);
    }

    @Operation(summary = "사용자 차단", description = "서비스 전체에 단방향 차단을 적용한다. 이미 차단한 회원이면 멱등하게 성공한다.")
    @SecurityRequirement(name = MEMBER_ID_SECURITY_SCHEME)
    @PutMapping("/api/members/me/blocks/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthMember Long blockerId,
                      @Parameter(description = "차단할 회원 ID") @PathVariable Long memberId) {
        memberBlockService.block(blockerId, memberId);
    }

    @Operation(summary = "사용자 차단 해제", description = "차단 관계가 없어도 멱등하게 성공한다.")
    @SecurityRequirement(name = MEMBER_ID_SECURITY_SCHEME)
    @DeleteMapping("/api/members/me/blocks/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@AuthMember Long blockerId,
                        @Parameter(description = "차단 해제할 회원 ID") @PathVariable Long memberId) {
        memberBlockService.unblock(blockerId, memberId);
    }

    @Operation(summary = "계정 삭제",
            description = "회원의 댓글, 모든 모임 참여 기록·진도, 양방향 차단 관계를 함께 하드 삭제한다.")
    @SecurityRequirement(name = MEMBER_ID_SECURITY_SCHEME)
    @DeleteMapping("/api/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@AuthMember Long memberId) {
        memberService.delete(memberId);
    }
}
