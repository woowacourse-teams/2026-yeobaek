package watson.backend.member;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/api/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberCreateResponse createMember(@RequestBody MemberCreateRequest request) {
        return memberService.create(request.nickname());
    }
}
