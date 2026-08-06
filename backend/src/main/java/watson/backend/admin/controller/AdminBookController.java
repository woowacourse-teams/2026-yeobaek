package watson.backend.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.admin.dto.BookUploadRequest;
import watson.backend.admin.dto.BookUploadResponse;
import watson.backend.admin.service.BookIngestService;

@RestController
@RequiredArgsConstructor
public class AdminBookController {

    private final BookIngestService bookIngestService;

    @PostMapping("/api/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookUploadResponse upload(@RequestBody BookUploadRequest request) {
        return bookIngestService.upload(request);
    }
}
