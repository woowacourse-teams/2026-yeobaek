package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminBookDashboardService {

    private final BookManagementRepository bookManagementRepository;
    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public AdminDashboardBooksResponse findBooksWithClubCounts() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.books").addKeyValue(PHASE, ATTEMPT)
                .log("관리자 도서 현황을 조회합니다.");
        Books books = new Books(bookManagementRepository.findAll());
        if (books.isEmpty()) {
            logSuccess(0);
            return new AdminDashboardBooksResponse(List.of());
        }
        Map<Long, Long> clubCounts = clubRepository.countClubsByBookIds(books.ids()).stream()
                .collect(Collectors.toMap(BookClubCount::getBookId, BookClubCount::getClubCount));
        var response = new AdminDashboardBooksResponse(books.asList().stream()
                .map(book -> new AdminDashboardBookResponse(
                        book.getId(),
                        book.getTitle().value(),
                        book.getStatus(),
                        clubCounts.getOrDefault(book.getId(), 0L)))
                .sorted(Comparator.comparingLong(AdminDashboardBookResponse::clubCount).reversed()
                        .thenComparing(AdminDashboardBookResponse::bookId))
                .toList());
        logSuccess(response.books().size());
        return response;
    }

    private void logSuccess(int resultCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.books").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("resultCount", resultCount).log("관리자 도서 현황을 조회했습니다.");
    }
}
