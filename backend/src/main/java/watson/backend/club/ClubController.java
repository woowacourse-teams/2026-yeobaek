package watson.backend.club;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.auth.AuthMember;

@RestController
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping("/api/clubs")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubCreateResponse create(@AuthMember Long memberId, @RequestBody ClubCreateRequest request) {
        return clubService.create(memberId, request.name(), request.bookId());
    }
}
