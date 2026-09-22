package yeobaek.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminDashboardBookResponse;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.book.repository.BookManagementRepository;

@Service
@RequiredArgsConstructor
public class AdminBookDashboardService {

    private final BookManagementRepository bookManagementRepository;

    @Transactional(readOnly = true)
    public AdminDashboardBooksResponse findBooks() {
        return new AdminDashboardBooksResponse(bookManagementRepository.findAdminDashboardStatistics().stream()
                .map(statistics -> new AdminDashboardBookResponse(
                        statistics.getBookId(),
                        statistics.getTitle(),
                        statistics.getStatus(),
                        statistics.getClubCount()))
                .toList());
    }
}
