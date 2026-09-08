package yeobaek.backend.admin.dto;

import java.util.List;

public record AdminBooksResponse(List<AdminBookResponse> books) {

    public AdminBooksResponse {
        books = List.copyOf(books);
    }
}
