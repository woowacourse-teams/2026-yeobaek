package yeobaek.backend.support;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.content.api.ContentLifecycleApi;
import yeobaek.backend.appreciation.api.AppreciationRootApi;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.space.api.SpaceRootLifecycleApi;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private ContentLifecycleApi contentLifecycleApi;

    @Autowired
    private SpaceRootLifecycleApi spaceRootLifecycleApi;

    @Autowired
    private AppreciationRootApi appreciationRootApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected Club newClub(ClubName name, JoinCode joinCode) {
        return new Club(spaceRootLifecycleApi.create("CLUB").value(), name, joinCode);
    }

    protected PublicRoom newPublicRoom() {
        return PublicRoom.create(spaceRootLifecycleApi.create("PUBLIC_ROOM").value());
    }

    protected Comment newClubComment(ClubMember membership, Sentence sentence, CommentContent content) {
        var root = appreciationRootApi.create("COMMENT", new MemberId(membership.getMemberId()), LocalDateTime.now());
        return CommentFixtures.inClub(root.id(), contentIdOf(sentence), membership, sentence, content);
    }

    protected Comment newClubComment(ClubMember membership, Sentence sentence, String content) {
        return newClubComment(membership, sentence, new CommentContent(content));
    }

    protected Comment newPublicRoomComment(PublicRoom room, Member author, Sentence sentence, CommentContent content) {
        var root = appreciationRootApi.create("COMMENT", new MemberId(author.getId()), LocalDateTime.now());
        return CommentFixtures.inPublicRoom(root.id(), contentIdOf(sentence), room, sentence, content);
    }

    protected Comment newPublicRoomComment(PublicRoom room, Member author, Sentence sentence, String content) {
        return newPublicRoomComment(room, author, sentence, new CommentContent(content));
    }

    protected Book newBook(BookTitle title, Publisher publisher, Integer publishedYear,
                           int passageCount, String coverImageKey) {
        ContentId contentId = contentLifecycleApi.createContent("BOOK");
        return new Book(contentId, title, publisher, publishedYear, passageCount, coverImageKey);
    }

    protected Passage newPassage(Chapter chapter, int sequence, List<SentenceContent> sentenceContents) {
        ContentId contentId = new ContentId(chapter.getBook().getContentId());
        ContentLocationId passageLocationId = contentLifecycleApi.createLocation(contentId, "PASSAGE");
        List<ContentLocationId> sentenceLocationIds = sentenceContents.stream()
                .map(ignored -> contentLifecycleApi.createLocation(contentId, "SENTENCE"))
                .toList();
        return new Passage(passageLocationId, sentenceLocationIds, chapter, sequence, sentenceContents);
    }

    private ContentId contentIdOf(Sentence sentence) {
        Long contentId = jdbcTemplate.queryForObject(
                "select content_id from content_locations where id = ?", Long.class, sentence.getLocationId());
        return new ContentId(contentId);
    }

    @BeforeEach
    void cleanDatabase() {
        CommentFixtures.clear();
        databaseCleaner.clean();
    }
}
