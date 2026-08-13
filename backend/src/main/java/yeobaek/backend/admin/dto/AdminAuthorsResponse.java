package yeobaek.backend.admin.dto;

import java.util.List;

public record AdminAuthorsResponse(List<AdminAuthorResponse> authors) {

    public AdminAuthorsResponse {
        authors = List.copyOf(authors);
    }
}
