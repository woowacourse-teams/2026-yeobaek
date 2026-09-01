package yeobaek.backend.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.member.dto.MemberCreateRequest;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@Tag(name = "회원")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AnalyticsTracker analyticsTracker;

    @Operation(summary = "회원 생성", description = "닉네임 입력만으로 회원을 생성하고 ID를 발급한다. 헤더 불필요(최초 진입).")
    @PostMapping("/api/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberCreateResponse createMember(@RequestBody MemberCreateRequest request) {
        MemberCreateResponse response = memberService.create(request.nickname());
        analyticsTracker.track(response.memberId(), AnalyticsEvent.memberCreated());
        return response;
    }
}
