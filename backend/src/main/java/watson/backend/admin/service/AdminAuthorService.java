package watson.backend.admin.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.admin.dto.AdminAuthorBookResponse;
import watson.backend.admin.dto.AdminAuthorResponse;
import watson.backend.admin.dto.AdminAuthorsResponse;
import watson.backend.book.domain.Author;
import watson.backend.book.repository.AuthorBookRepository;
import watson.backend.book.repository.AuthorRepository;

@Service
@RequiredArgsConstructor
public class AdminAuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;

    @Transactional(readOnly = true)
    public AdminAuthorsResponse findAuthors() {
        List<Author> authors = authorRepository.findAllByOrderByIdAsc();
        if (authors.isEmpty()) {
            return new AdminAuthorsResponse(List.of());
        }
        Map<Long, List<AdminAuthorBookResponse>> booksByAuthorId = booksByAuthorId(
                authors.stream().map(Author::getId).toList());
        return new AdminAuthorsResponse(authors.stream()
                .map(author -> AdminAuthorResponse.of(author,
                        booksByAuthorId.getOrDefault(author.getId(), List.of())))
                .toList());
    }

    private Map<Long, List<AdminAuthorBookResponse>> booksByAuthorId(List<Long> authorIds) {
        return authorBookRepository.findAllWithBookByAuthorIdIn(authorIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getAuthor().getId(),
                        Collectors.mapping(authorBook -> AdminAuthorBookResponse.of(authorBook.getBook()),
                                Collectors.toList())));
    }
}
