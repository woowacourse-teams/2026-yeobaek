package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardBookResponse;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.content.api.book.BookAdministrationApi;
import yeobaek.backend.readmodel.admin.AdminClubStatisticsReadModel;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBookDashboardService {

    private final BookAdministrationApi bookAdministrationApi;
    private final AdminClubStatisticsReadModel statisticsReadModel;

    @Transactional(readOnly = true)
    public AdminDashboardBooksResponse findBooksWithClubCounts() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findBooksWithClubCounts")
                .log("관리자 도서 현황을 조회합니다.");
        List<BookAdministrationApi.BookStatusView> books = bookAdministrationApi.findBookStatuses();
        if (books.isEmpty()) {
            logSuccess(0);
            return new AdminDashboardBooksResponse(List.of());
        }
        Map<Long, Long> clubCounts = statisticsReadModel.countClubsByBookIds(
                books.stream().map(BookAdministrationApi.BookStatusView::bookId).toList());
        var response = new AdminDashboardBooksResponse(books.stream()
                .map(book -> new AdminDashboardBookResponse(
                        book.bookId(), book.title(), book.status(),
                        clubCounts.getOrDefault(book.bookId(), 0L)))
                .sorted(Comparator.comparingLong(AdminDashboardBookResponse::clubCount).reversed()
                        .thenComparing(AdminDashboardBookResponse::bookId))
                .toList());
        logSuccess(response.books().size());
        return response;
    }

    private void logSuccess(int resultCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findBooksWithClubCounts").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", resultCount).log("관리자 도서 현황을 조회했습니다.");
    }
}
