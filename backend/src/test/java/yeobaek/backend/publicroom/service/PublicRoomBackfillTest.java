package yeobaek.backend.publicroom.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.IntegrationTest;

class PublicRoomBackfillTest extends IntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private PublicRoomBackfill publicRoomBackfill;

    @Test
    @DisplayName("기존 도서 backfill은 재실행해도 도서마다 공개방 하나를 유지한다")
    void backfillExistingBooks() {
        Book first = bookRepository.save(new Book(new BookTitle("기존 책 1"), null, null, 1, null));
        Book second = bookRepository.save(new Book(new BookTitle("기존 책 2"), null, null, 1, null));

        publicRoomBackfill.run(new DefaultApplicationArguments());
        publicRoomBackfill.run(new DefaultApplicationArguments());

        assertThat(publicRoomRepository.findByBookId(first.getId())).isPresent();
        assertThat(publicRoomRepository.findByBookId(second.getId())).isPresent();
        assertThat(publicRoomRepository.count()).isEqualTo(2);
    }
}
