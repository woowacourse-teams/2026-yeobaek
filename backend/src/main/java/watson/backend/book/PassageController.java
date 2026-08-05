package watson.backend.book;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.auth.AuthMember;

@RestController
@RequiredArgsConstructor
public class PassageController {

    private final PassageService passageService;

    @GetMapping("/api/clubs/{clubId}/passages")
    public PassagesResponse findPassages(@AuthMember Long memberId,
                                         @PathVariable Long clubId,
                                         @RequestParam int from,
                                         @RequestParam int to) {
        return passageService.findPassages(memberId, clubId, from, to);
    }
}
