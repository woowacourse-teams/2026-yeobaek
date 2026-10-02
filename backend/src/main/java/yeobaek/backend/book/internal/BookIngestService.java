package yeobaek.backend.book.internal;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.api.BookIngestApi;
import yeobaek.backend.book.api.BookIngestFailure;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Authors;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Books;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.Isni;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.content.api.ContentLifecycleApi;
import yeobaek.backend.foundation.identity.ContentId;

@Service("bookModuleIngestService")
@RequiredArgsConstructor
public class BookIngestService implements BookIngestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger("yeobaek.backend.admin.service.BookIngestService");
    private static final int MAX_CONTENT_BYTES = 65_535;

    private final ActiveBookRepository activeBookRepository;
    private final BookManagementRepository bookManagementRepository;
    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;
    private final ContentLifecycleApi contentLifecycleApi;

    @Override
    @Transactional
    public Result ingest(Command command) {
        validateStructure(command);
        ContentId contentId = contentLifecycleApi.createContent(yeobaek.backend.content.domain.Book.KIND);
        Book book = new Book(contentId, new BookTitle(command.title()), publisher(command.publisher()),
                command.publishedYear(), countPassages(command), command.coverImageKey());
        Authors authors = resolveAuthors(command.authors());
        rejectDuplicateBook(book, authors);

        bookManagementRepository.save(book);
        saveAuthors(book, authors);
        saveChapters(contentId, book, command.chapters());
        return new Result(book.getId(), contentId, book.getTitle().value(), book.getCoverImageKey(),
                book.getPassageCount().value());
    }

    private Publisher publisher(String value) {
        return value == null ? null : new Publisher(value);
    }

    private void saveAuthors(Book book, Authors authors) {
        logPersistenceAttempt("admin.book.saveAuthors", authors.asList().size());
        for (Author author : authors.asList()) {
            if (author.getId() == null) {
                authorRepository.save(author);
            }
            authorBookRepository.save(new AuthorBook(author, book));
        }
        logPersistenceSuccess("admin.book.saveAuthors", authors.asList().size());
    }

    private void validateStructure(Command command) {
        if (command.authors() == null || command.authors().isEmpty()) {
            throw failure(BookIngestFailure.Reason.AUTHORS_EMPTY, "작가는 최소 1명이어야 합니다.",
                    Map.of(REASON, "authors_empty"));
        }
        if (command.chapters() == null || command.chapters().isEmpty()) {
            throw failure(BookIngestFailure.Reason.CHAPTERS_EMPTY, "목차는 최소 1개여야 합니다.",
                    Map.of(REASON, "chapters_empty"));
        }
        for (ChapterInput chapter : command.chapters()) {
            if (chapter.passages() == null || chapter.passages().isEmpty()) {
                throw failure(BookIngestFailure.Reason.PASSAGES_EMPTY, "각 목차의 본문은 최소 1개여야 합니다.",
                        Map.of(REASON, "passages_empty"));
            }
            chapter.passages().forEach(this::validatePassage);
        }
    }

    private void validatePassage(PassageInput passage) {
        if (passage.sentences() == null || passage.sentences().isEmpty()) {
            throw failure(BookIngestFailure.Reason.SENTENCES_EMPTY, "각 문단의 문장은 최소 1개여야 합니다.",
                    Map.of(REASON, "sentences_empty"));
        }
        passage.sentences().forEach(this::validateSentence);
    }

    private void validateSentence(SentenceInput sentence) {
        if (sentence.content().getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw failure(BookIngestFailure.Reason.SENTENCE_TOO_LARGE,
                    "문장 하나는 " + MAX_CONTENT_BYTES + "바이트를 넘을 수 없습니다.",
                    Map.of(REASON, "sentence_too_large", "maxBytes", Integer.toString(MAX_CONTENT_BYTES)));
        }
    }

    private int countPassages(Command command) {
        return command.chapters().stream().mapToInt(chapter -> chapter.passages().size()).sum();
    }

    private Authors resolveAuthors(List<AuthorReference> references) {
        List<Author> resolved = new ArrayList<>();
        Set<Long> seenAuthorIds = new HashSet<>();
        Set<Isni> seenIsnis = new HashSet<>();
        for (AuthorReference reference : references) {
            Author author = resolve(reference);
            rejectDuplicateEntry(author, seenAuthorIds, seenIsnis);
            resolved.add(author);
        }
        return new Authors(resolved);
    }

    private Author resolve(AuthorReference reference) {
        if (reference.referencesExisting()) {
            if (reference.name() != null || reference.isni() != null) {
                throw failure(BookIngestFailure.Reason.MIXED_AUTHOR_REFERENCE,
                        "작가 항목은 {name, isni?} 또는 {authorId} 중 한 형태여야 합니다.",
                        Map.of(REASON, "mixed_author_reference", "authorId", reference.authorId().toString()));
            }
            return authorRepository.findById(reference.authorId())
                    .orElseThrow(() -> failure(BookIngestFailure.Reason.AUTHOR_NOT_FOUND,
                            "authorId가 가리키는 작가가 존재하지 않습니다: authorId=" + reference.authorId(),
                            Map.of("authorId", reference.authorId().toString())));
        }
        AuthorName name = new AuthorName(reference.name());
        if (reference.isni() == null) {
            return new Author(name);
        }
        Isni isni = new Isni(reference.isni());
        return authorRepository.findByIsni(isni.value())
                .map(existing -> requireSameName(existing, name))
                .orElseGet(() -> new Author(name, isni));
    }

    private Author requireSameName(Author existing, AuthorName requestedName) {
        if (!existing.hasSameName(requestedName)) {
            throw failure(BookIngestFailure.Reason.AUTHOR_NAME_MISMATCH,
                    "ISNI로 찾은 기존 작가와 요청한 작가 이름이 일치하지 않습니다.",
                    Map.of(REASON, "author_name_mismatch"));
        }
        return existing;
    }

    private void rejectDuplicateEntry(Author author, Set<Long> seenAuthorIds, Set<Isni> seenIsnis) {
        if (author.getId() != null && !seenAuthorIds.add(author.getId())) {
            throw failure(BookIngestFailure.Reason.DUPLICATE_AUTHOR,
                    "한 업로드 요청에 같은 작가가 중복 기재되었습니다: authorId=" + author.getId(),
                    Map.of("authorId", author.getId().toString(), REASON, "duplicate_author"));
        }
        if (author.getIsni() != null && !seenIsnis.add(author.getIsni())) {
            throw failure(BookIngestFailure.Reason.DUPLICATE_AUTHOR_ISNI,
                    "한 업로드 요청에 같은 작가가 중복 기재되었습니다: isni=" + author.getIsni().value(),
                    Map.of(REASON, "duplicate_author_isni"));
        }
    }

    private void rejectDuplicateBook(Book book, Authors authors) {
        if (authors.containsUnsavedAuthor()) {
            return;
        }
        BookDeduplicationKey key = book.deduplicationKey(authors.ids());
        Books candidates = new Books(activeBookRepository.findAllByTitle(book.getTitle()));
        Map<Long, Set<Long>> authorIdsByBookId = authorIdsByBookId(candidates);
        if (candidates.containsDuplicateOf(key, authorIdsByBookId)) {
            throw failure(BookIngestFailure.Reason.DUPLICATE_BOOK,
                    "동일한 서지 정보와 작가 구성의 활성 도서가 이미 존재합니다.",
                    Map.of(REASON, "duplicate_book"));
        }
    }

    private Map<Long, Set<Long>> authorIdsByBookId(Books candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        return authorBookRepository.findAllWithAuthorByBookIdIn(candidates.ids()).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getId(), Collectors.toSet())));
    }

    private void saveChapters(ContentId contentId, Book book, List<ChapterInput> chapters) {
        logPersistenceAttempt("admin.book.saveChapters", chapters.size());
        int passageSequence = 1;
        int chapterSequence = 1;
        for (ChapterInput chapterInput : chapters) {
            Chapter chapter = chapterRepository.save(
                    new Chapter(book, new ChapterTitle(chapterInput.title()), chapterSequence));
            chapterSequence++;
            for (PassageInput passageInput : chapterInput.passages()) {
                var passageLocationId = contentLifecycleApi.createLocation(contentId, "PASSAGE");
                var sentenceLocationIds = passageInput.sentences().stream()
                        .map(ignored -> contentLifecycleApi.createLocation(contentId, "SENTENCE"))
                        .toList();
                passageRepository.save(new Passage(passageLocationId, sentenceLocationIds, chapter, passageSequence,
                        passageInput.sentences().stream()
                                .map(sentence -> new SentenceContent(sentence.content())).toList()));
                passageSequence++;
            }
        }
        logPersistenceSuccess("admin.book.saveChapters", chapters.size());
    }

    private BookIngestFailure failure(BookIngestFailure.Reason reason, String message,
                                      Map<String, String> context) {
        return new BookIngestFailure(reason, message, context);
    }

    private void logPersistenceAttempt(String operation, int itemCount) {
        LOGGER.atInfo().addKeyValue(OPERATION, operation)
                .addKeyValue("itemCount", itemCount).log("도서 업로드 영속성 작업을 시작합니다.");
    }

    private void logPersistenceSuccess(String operation, int itemCount) {
        LOGGER.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue("itemCount", itemCount).log("도서 업로드 영속성 작업을 완료했습니다.");
    }
}
