package yeobaek.backend.comment.repository;

import yeobaek.backend.support.CommentFixtures;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.appreciation.api.CommentQueryApi;
import yeobaek.backend.appreciation.persistence.AppreciationRepository;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.support.IntegrationTest;

class CommentRepositoryTest extends IntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AppreciationContextRepository appreciationContextRepository;

    @Autowired
    private CommentQueryApi commentQueryApi;

    @Autowired
    private AppreciationRepository appreciationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("댓글을 저장하면 작성일이 자동으로 기록되고 수정일은 비어 있다")
    void saveSetsCreatedAt() {
        Book book = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), null, 1924, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, Collections.singletonList(new SentenceContent("본문"))));
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE03")));
        bind(club, book);
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(member.getId()), club));

        Comment saved = saveComment(
                newClubComment(clubMember, passage.getSentences().getFirst(), new CommentContent("이 문장에서 멈칫했어요.")));
        commentRepository.flush();

        var root = appreciationRepository.findById(saved.getId()).orElseThrow();
        assertThat(root.getCreatedAt()).isNotNull();
        assertThat(root.getUpdatedAt()).isNull();
        assertThat(root.getAuthorId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("수정한 댓글 값 객체를 저장한 뒤 다시 조회한다")
    void reloadsUpdatedCommentContent() {
        Book book = bookRepository.save(newBook(new BookTitle("댓글 수정 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, Collections.singletonList(new SentenceContent("본문"))));
        Member member = memberRepository.save(new Member(new Nickname("수정자")));
        Club club = clubRepository.save(newClub(new ClubName("수정 모임"), new JoinCode("EDIT01")));
        bind(club, book);
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(new yeobaek.backend.foundation.identity.MemberId(member.getId()), club));
        Comment comment = saveComment(
                newClubComment(clubMember, passage.getSentences().getFirst(), new CommentContent("수정 전")));
        commentRepository.flush();

        comment.updateContent(new CommentContent("  수정 후  "));
        commentRepository.saveAndFlush(comment);
        entityManager.clear();

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getContent())
                .isEqualTo("  수정 후  ");
    }

    @Test
    @DisplayName("조회할 문장 ID에 대해 현재 모임의 댓글 수만 집계한다")
    void countsCommentsByClubAndSentenceIds() {
        Book book = bookRepository.save(newBook(new BookTitle("댓글 집계 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(newPassage(chapter, 1, java.util.List.of(new SentenceContent("첫 문장."), new SentenceContent("둘째 문장."))));
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("COUNT1")));
        Club otherClub = clubRepository.save(newClub(new ClubName("2기"), new JoinCode("COUNT2")));
        bind(club, book);
        bind(otherClub, book);
        ClubMember membership = clubMemberRepository.save(new ClubMember(new MemberId(member.getId()), club));
        ClubMember otherMembership = clubMemberRepository.save(
                new ClubMember(new MemberId(member.getId()), otherClub));
        var firstSentence = passage.getSentences().getFirst();
        var secondSentence = passage.getSentences().get(1);
        saveComment(newClubComment(membership, firstSentence, new CommentContent("우리 모임 댓글 1")));
        saveComment(newClubComment(membership, firstSentence, new CommentContent("우리 모임 댓글 2")));
        saveComment(newClubComment(otherMembership, firstSentence, new CommentContent("다른 모임 댓글")));

        var counts = commentQueryApi.countByLocation(
                new SpaceId(club.getSpaceId()), java.util.List.of(
                        new ContentLocationId(firstSentence.getLocationId()),
                        new ContentLocationId(secondSentence.getLocationId())), java.util.Set.of());

        assertThat(counts).containsOnlyKeys(new ContentLocationId(firstSentence.getLocationId()));
        assertThat(counts.get(new ContentLocationId(firstSentence.getLocationId()))).isEqualTo(2L);
    }

    private Comment saveComment(Comment comment) {
        Comment saved = commentRepository.save(comment);
        appreciationContextRepository.save(CommentFixtures.contextOf(saved));
        return saved;
    }

    private void bind(Club club, Book book) {
        bindingApi.bind(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId()));
    }
}
