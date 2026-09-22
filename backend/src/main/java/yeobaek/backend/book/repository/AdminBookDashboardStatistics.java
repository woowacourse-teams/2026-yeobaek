package yeobaek.backend.book.repository;

import yeobaek.backend.book.domain.BookStatus;

public interface AdminBookDashboardStatistics {

    Long getBookId();

    String getTitle();

    BookStatus getStatus();

    long getClubCount();
}
