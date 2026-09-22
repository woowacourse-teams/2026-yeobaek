package yeobaek.backend.club.repository;

import yeobaek.backend.book.domain.BookStatus;

public interface AdminClubDashboardStatistics {

    Long getClubId();

    String getName();

    Long getBookId();

    String getBookTitle();

    BookStatus getBookStatus();

    long getMemberCount();

    long getCommentCount();
}
