package watson.backend.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.admin.dto.AuthorEntryRequest;
import watson.backend.admin.dto.BookUploadRequest;
import watson.backend.admin.dto.BookUploadResponse;
import watson.backend.admin.dto.ChapterUploadRequest;
import watson.backend.admin.dto.PassageUploadRequest;
import watson.backend.book.domain.Author;
import watson.backend.book.domain.AuthorBook;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Chapter;
import watson.backend.book.domain.Passage;
import watson.backend.book.repository.AuthorBookRepository;
import watson.backend.book.repository.AuthorRepository;
import watson.backend.book.repository.BookRepository;
import watson.backend.book.repository.ChapterRepository;
import watson.backend.book.repository.PassageRepository;
import watson.backend.support.BadRequestException;
import watson.backend.support.ErrorCode;
import watson.backend.support.NotFoundException;

/**
 * 인제스트 규격 JSON(API.md 6장) 업로드. 단일 트랜잭션이므로 실패 시 아무것도 저장되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class BookIngestService {

    private static final int MAX_CONTENT_BYTES = 65_535;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;

    @Transactional
    public BookUploadResponse upload(BookUploadRequest request) {
        validateStructure(request);
        Book book = new Book(request.title(), request.publisher(), request.publishedYear(), countPassages(request));
        List<Author> authors = resolveAuthors(request.authors());
        rejectDuplicateBook(book, authors);

        bookRepository.save(book);
        for (Author author : authors) {
            if (author.getId() == null) {
                authorRepository.save(author);
            }
            authorBookRepository.save(new AuthorBook(author, book));
        }
        saveChapters(book, request.chapters());
        return new BookUploadResponse(book.getId(), book.getTitle(), book.getPassageCount());
    }

    private void validateStructure(BookUploadRequest request) {
        if (request.authors().isEmpty()) {
            throw new IllegalArgumentException("작가는 최소 1명이어야 합니다.");
        }
        if (request.chapters().isEmpty()) {
            throw new IllegalArgumentException("목차는 최소 1개여야 합니다.");
        }
        for (ChapterUploadRequest chapter : request.chapters()) {
            if (chapter.passages().isEmpty()) {
                throw new IllegalArgumentException("각 목차의 본문은 최소 1개여야 합니다.");
            }
            chapter.passages().forEach(this::validateContent);
        }
    }

    private void validateContent(PassageUploadRequest passage) {
        String content = passage.content();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("본문 내용은 공백이 아니어야 합니다.");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException("본문 하나는 " + MAX_CONTENT_BYTES + "바이트를 넘을 수 없습니다.");
        }
    }

    private int countPassages(BookUploadRequest request) {
        return request.chapters().stream().mapToInt(chapter -> chapter.passages().size()).sum();
    }

    private List<Author> resolveAuthors(List<AuthorEntryRequest> entries) {
        List<Author> resolved = new ArrayList<>();
        Set<Long> seenAuthorIds = new HashSet<>();
        Set<String> seenIsnis = new HashSet<>();
        for (AuthorEntryRequest entry : entries) {
            Author author = resolve(entry);
            rejectDuplicateEntry(author, seenAuthorIds, seenIsnis);
            resolved.add(author);
        }
        return resolved;
    }

    private Author resolve(AuthorEntryRequest entry) {
        if (entry.referencesExisting()) {
            if (entry.name() != null || entry.isni() != null) {
                throw new IllegalArgumentException("작가 항목은 {name, isni?} 또는 {authorId} 중 한 형태여야 합니다.");
            }
            return authorRepository.findById(entry.authorId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.AUTHOR_NOT_FOUND));
        }
        if (entry.isni() == null) {
            return new Author(entry.name());
        }
        String isni = Author.normalizeIsni(entry.isni());
        return authorRepository.findByIsni(isni)
                .map(existing -> requireSameName(existing, entry.name()))
                .orElseGet(() -> new Author(entry.name(), isni));
    }

    private Author requireSameName(Author existing, String name) {
        if (!existing.hasSameName(name)) {
            throw new BadRequestException(ErrorCode.AUTHOR_NAME_MISMATCH);
        }
        return existing;
    }

    private void rejectDuplicateEntry(Author author, Set<Long> seenAuthorIds, Set<String> seenIsnis) {
        if (author.getId() != null && !seenAuthorIds.add(author.getId())) {
            throw new BadRequestException(ErrorCode.DUPLICATE_AUTHOR);
        }
        if (author.getIsni() != null && !seenIsnis.add(author.getIsni())) {
            throw new BadRequestException(ErrorCode.DUPLICATE_AUTHOR);
        }
    }

    private void rejectDuplicateBook(Book book, List<Author> authors) {
        if (authors.stream().anyMatch(author -> author.getId() == null)) {
            return;
        }
        Set<Long> authorIds = authors.stream().map(Author::getId).collect(Collectors.toSet());
        boolean duplicate = bookRepository.findAllByTitle(book.getTitle()).stream()
                .filter(candidate -> candidate.hasSameBibliography(book))
                .anyMatch(candidate -> authorIdsOf(candidate).equals(authorIds));
        if (duplicate) {
            throw new BadRequestException(ErrorCode.DUPLICATE_BOOK);
        }
    }

    private Set<Long> authorIdsOf(Book book) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getId())
                .collect(Collectors.toSet());
    }

    private void saveChapters(Book book, List<ChapterUploadRequest> chapters) {
        int passageSequence = 1;
        int chapterSequence = 1;
        for (ChapterUploadRequest chapterRequest : chapters) {
            Chapter chapter = chapterRepository.save(new Chapter(book, chapterRequest.title(), chapterSequence));
            chapterSequence++;
            for (PassageUploadRequest passageRequest : chapterRequest.passages()) {
                passageRepository.save(new Passage(chapter, passageSequence, passageRequest.content()));
                passageSequence++;
            }
        }
    }
}
