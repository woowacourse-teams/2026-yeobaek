package yeobaek.backend.admin.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardBookResponse;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.book.domain.Books;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.repository.BookClubCount;
import yeobaek.backend.club.repository.ClubRepository;

@Service
@RequiredArgsConstructor
public class AdminBookDashboardService {

    private final BookManagementRepository bookManagementRepository;
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public AdminDashboardBooksResponse findBooks() {
        Books books = new Books(bookManagementRepository.findAll());
        if (books.isEmpty()) {
            return new AdminDashboardBooksResponse(List.of());
        }
        Map<Long, Long> clubCounts = clubRepository.countByBookIds(books.ids()).stream()
                .collect(Collectors.toMap(BookClubCount::getBookId, BookClubCount::getClubCount));
        return new AdminDashboardBooksResponse(books.asList().stream()
                .map(book -> new AdminDashboardBookResponse(
                        book.getId(),
                        book.getTitle().value(),
                        book.getStatus(),
                        clubCounts.getOrDefault(book.getId(), 0L)))
                .sorted(Comparator.comparingLong(AdminDashboardBookResponse::clubCount).reversed()
                        .thenComparing(AdminDashboardBookResponse::bookId))
                .toList());
    }
}
