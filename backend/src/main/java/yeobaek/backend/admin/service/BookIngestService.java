package yeobaek.backend.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Authors;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Books;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;
import yeobaek.backend.book.domain.vo.Isni;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

/**
 * 인제스트 규격 JSON(API.md 6장) 업로드. 단일 트랜잭션이므로 실패 시 아무것도 저장되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class BookIngestService {

    private static final int MAX_CONTENT_BYTES = 65_535;

    private final ActiveBookRepository activeBookRepository;
    private final BookManagementRepository bookManagementRepository;
    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional
    public BookUploadResponse upload(BookUploadRequest request) {
        validateStructure(request);
        Book book = new Book(request.title(), request.publisher(), request.publishedYear(), countPassages(request),
                request.coverImageKey());
        Authors authors = resolveAuthors(request.authors());
        rejectDuplicateBook(book, authors);

        bookManagementRepository.save(book);
        for (Author author : authors.asList()) {
            if (author.getId() == null) {
                authorRepository.save(author);
            }
            authorBookRepository.save(new AuthorBook(author, book));
        }
        saveChapters(book, request.chapters());
        return new BookUploadResponse(book.getId(), book.getTitle().value(),
                bookCoverUrlResolver.resolve(book.getCoverImageKey()), book.getPassageCount().value());
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
            chapter.passages().forEach(this::validatePassage);
        }
    }

    private void validatePassage(PassageUploadRequest passage) {
        if (passage.sentences().isEmpty()) {
            throw new IllegalArgumentException("각 문단의 문장은 최소 1개여야 합니다.");
        }
        passage.sentences().forEach(this::validateSentence);
    }

    private void validateSentence(SentenceUploadRequest sentence) {
        SentenceContent content = sentence.content();
        if (content.value().getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException("문장 하나는 " + MAX_CONTENT_BYTES + "바이트를 넘을 수 없습니다.");
        }
    }

    private int countPassages(BookUploadRequest request) {
        return request.chapters().stream().mapToInt(chapter -> chapter.passages().size()).sum();
    }

    private Authors resolveAuthors(List<AuthorEntryRequest> entries) {
        List<Author> resolved = new ArrayList<>();
        Set<Long> seenAuthorIds = new HashSet<>();
        Set<Isni> seenIsnis = new HashSet<>();
        for (AuthorEntryRequest entry : entries) {
            Author author = resolve(entry);
            rejectDuplicateEntry(author, seenAuthorIds, seenIsnis);
            resolved.add(author);
        }
        return new Authors(resolved);
    }

    private Author resolve(AuthorEntryRequest entry) {
        if (entry.referencesExisting()) {
            if (entry.name() != null || entry.isni() != null) {
                throw new IllegalArgumentException("작가 항목은 {name, isni?} 또는 {authorId} 중 한 형태여야 합니다.");
            }
            return authorRepository.findById(entry.authorId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.AUTHOR_NOT_FOUND,
                            "authorId가 가리키는 작가가 존재하지 않습니다: authorId=" + entry.authorId()));
        }
        if (entry.isni() == null) {
            return new Author(entry.name());
        }
        Isni isni = entry.isni();
        return authorRepository.findByIsni(isni.value())
                .map(existing -> requireSameName(existing, entry.name()))
                .orElseGet(() -> new Author(entry.name(), isni));
    }

    private Author requireSameName(Author existing, AuthorName requestedName) {
        if (!existing.hasSameName(requestedName)) {
            throw authorNameMismatch();
        }
        return existing;
    }

    private BadRequestException authorNameMismatch() {
        return new BadRequestException(
                ErrorCode.AUTHOR_NAME_MISMATCH,
                "ISNI로 찾은 기존 작가와 요청한 작가 이름이 일치하지 않습니다.");
    }

    private void rejectDuplicateEntry(Author author, Set<Long> seenAuthorIds, Set<Isni> seenIsnis) {
        if (author.getId() != null && !seenAuthorIds.add(author.getId())) {
            throw new BadRequestException(
                    ErrorCode.DUPLICATE_AUTHOR,
                    "한 업로드 요청에 같은 작가가 중복 기재되었습니다: authorId=" + author.getId());
        }
        if (author.getIsni() != null && !seenIsnis.add(author.getIsni())) {
            throw new BadRequestException(
                    ErrorCode.DUPLICATE_AUTHOR,
                    "한 업로드 요청에 같은 작가가 중복 기재되었습니다: isni=" + author.getIsni().value());
        }
    }

    private void rejectDuplicateBook(Book book, Authors authors) {
        if (authors.containsUnsavedAuthor()) {
            return;
        }
        BookDeduplicationKey key = book.deduplicationKey(authors.ids());
        Books candidates = new Books(
                activeBookRepository.findAllByTitle(book.getTitle()));
        Map<Long, Set<Long>> authorIdsByBookId = authorIdsByBookId(candidates);
        if (candidates.containsDuplicateOf(key, authorIdsByBookId)) {
            throw new BadRequestException(
                    ErrorCode.DUPLICATE_BOOK,
                    "동일한 서지 정보와 작가 구성의 활성 도서가 이미 존재합니다.");
        }
    }

    private Map<Long, Set<Long>> authorIdsByBookId(Books candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        return authorBookRepository.findAllWithAuthorByBookIdIn(candidates.ids()).stream()
                .collect(Collectors.groupingBy(
                        authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getId(), Collectors.toSet())));
    }

    private void saveChapters(Book book, List<ChapterUploadRequest> chapters) {
        int passageSequence = 1;
        int chapterSequence = 1;
        for (ChapterUploadRequest chapterRequest : chapters) {
            Chapter chapter = chapterRepository.save(new Chapter(book, chapterRequest.title(), chapterSequence));
            chapterSequence++;
            for (PassageUploadRequest passageRequest : chapterRequest.passages()) {
                passageRepository.save(new Passage(chapter, passageSequence, passageRequest.sentences().stream()
                        .map(SentenceUploadRequest::content)
                        .toList()));
                passageSequence++;
            }
        }
    }
}
