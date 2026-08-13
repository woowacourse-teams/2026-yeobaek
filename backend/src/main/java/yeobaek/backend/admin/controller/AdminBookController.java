package yeobaek.backend.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.service.BookIngestService;

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
