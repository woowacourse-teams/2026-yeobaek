package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminAuthorBookResponse;
import yeobaek.backend.admin.dto.AdminAuthorResponse;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional(readOnly = true)
    public AdminAuthorsResponse findAuthors() {
        log.atInfo().addKeyValue(OPERATION, "admin.author.findAll").addKeyValue(PHASE, ATTEMPT)
                .log("관리자 작가 목록을 조회합니다.");
        List<Author> authors = authorRepository.findAllByOrderByIdAsc();
        if (authors.isEmpty()) {
            logSuccess(0);
            return new AdminAuthorsResponse(List.of());
        }
        Map<Long, List<AdminAuthorBookResponse>> booksByAuthorId = booksByAuthorId(
                authors.stream().map(Author::getId).toList());
        var response = new AdminAuthorsResponse(authors.stream()
                .map(author -> AdminAuthorResponse.of(author,
                        booksByAuthorId.getOrDefault(author.getId(), List.of())))
                .toList());
        logSuccess(response.authors().size());
        return response;
    }

    private void logSuccess(int resultCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.author.findAll").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("resultCount", resultCount).log("관리자 작가 목록을 조회했습니다.");
    }

    private Map<Long, List<AdminAuthorBookResponse>> booksByAuthorId(List<Long> authorIds) {
        return authorBookRepository.findAllWithBookByAuthorIdIn(authorIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getAuthor().getId(),
                        Collectors.mapping(authorBook -> AdminAuthorBookResponse.of(authorBook.getBook(),
                                        bookCoverUrlResolver.resolve(authorBook.getBook().getCoverImageKey())),
                                Collectors.toList())));
    }
}
