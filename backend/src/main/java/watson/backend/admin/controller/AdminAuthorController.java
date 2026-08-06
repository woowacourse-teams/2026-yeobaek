package watson.backend.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.admin.dto.AdminAuthorsResponse;
import watson.backend.admin.service.AdminAuthorService;

@RestController
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AdminAuthorService adminAuthorService;

    @GetMapping("/api/admin/authors")
    public AdminAuthorsResponse findAuthors() {
        return adminAuthorService.findAuthors();
    }
}
