package yeobaek.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.repository.BookManagementRepository;

@Service
@RequiredArgsConstructor
public class AdminBookService {

    private final BookManagementRepository bookManagementRepository;

    @Transactional
    public void delete(Long bookId) {
        bookManagementRepository.delete(bookId);
    }
}
