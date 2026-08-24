package yeobaek.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.repository.BookArchiveRepository;

@Service
@RequiredArgsConstructor
public class AdminBookService {

    private final BookArchiveRepository bookArchiveRepository;

    @Transactional
    public void delete(Long bookId) {
        bookArchiveRepository.delete(bookId);
    }
}
