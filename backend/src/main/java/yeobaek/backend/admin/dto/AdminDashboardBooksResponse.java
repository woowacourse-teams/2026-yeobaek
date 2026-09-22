package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminDashboardBooksResponse(
        @Schema(description = "도서별 모임 현황") List<AdminDashboardBookResponse> books
) {

    public AdminDashboardBooksResponse {
        books = List.copyOf(books);
    }
}
