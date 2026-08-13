package yeobaek.backend.book.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.service.PassageService;

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
