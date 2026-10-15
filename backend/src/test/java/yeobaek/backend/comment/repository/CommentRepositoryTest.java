package yeobaek.backend.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class CommentRepositoryTest extends IntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

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

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("댓글을 저장하면 작성일이 자동으로 기록되고 수정일은 비어 있다")
    void saveSetsCreatedAt() {
        Book book = bookRepository.save(new Book(new BookTitle("운수 좋은 날"), null, 1924, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, Collections.singletonList(new SentenceContent("본문"))));
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(new Club(new ClubName("1기"), book, new JoinCode("CODE03")));
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(member, club));

        Comment saved = commentRepository.saveAndFlush(
                new Comment(clubMember, passage.getSentences().getFirst(), new CommentContent("이 문장에서 멈칫했어요.")));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.getWriter().getNickname()).isEqualTo("민서");
    }

    @Test
    @DisplayName("수정한 댓글 값 객체를 저장한 뒤 다시 조회한다")
    void reloadsUpdatedCommentContent() {
        Book book = bookRepository.save(new Book(new BookTitle("댓글 수정 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, Collections.singletonList(new SentenceContent("본문"))));
        Member member = memberRepository.save(new Member(new Nickname("수정자")));
        Club club = clubRepository.save(new Club(new ClubName("수정 모임"), book, new JoinCode("EDIT01")));
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(member, club));
        Comment comment = commentRepository.saveAndFlush(
                new Comment(clubMember, passage.getSentences().getFirst(), new CommentContent("수정 전")));

        comment.updateContent(new CommentContent("  수정 후  "));
        commentRepository.saveAndFlush(comment);
        entityManager.clear();

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getContent())
                .isEqualTo("  수정 후  ");
    }

    @Test
    @DisplayName("조회할 문장 ID에 대해 현재 모임의 댓글 수만 집계한다")
    void countsCommentsByClubAndSentenceIds() {
        Book book = bookRepository.save(new Book(new BookTitle("댓글 집계 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, java.util.List.of(new SentenceContent("첫 문장."), new SentenceContent("둘째 문장."))));
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(new Club(new ClubName("1기"), book, new JoinCode("COUNT1")));
        Club otherClub = clubRepository.save(new Club(new ClubName("2기"), book, new JoinCode("COUNT2")));
        ClubMember membership = clubMemberRepository.save(new ClubMember(member, club));
        ClubMember otherMembership = clubMemberRepository.save(new ClubMember(member, otherClub));
        var firstSentence = passage.getSentences().getFirst();
        var secondSentence = passage.getSentences().get(1);
        commentRepository.save(new Comment(membership, firstSentence, new CommentContent("우리 모임 댓글 1")));
        commentRepository.save(new Comment(membership, firstSentence, new CommentContent("우리 모임 댓글 2")));
        commentRepository.save(new Comment(otherMembership, firstSentence, new CommentContent("다른 모임 댓글")));

        var counts = commentRepository.countVisibleByMemberIdAndClubIdAndSentenceIdIn(
                member.getId(), club.getId(), java.util.List.of(firstSentence.getId(), secondSentence.getId()));

        assertThat(counts).singleElement()
                .satisfies(count -> {
                    assertThat(count.getSentenceId()).isEqualTo(firstSentence.getId());
                    assertThat(count.getCommentCount()).isEqualTo(2);
                });
    }

    @Test
    @DisplayName("모임 댓글과 문장 집계에 필요한 연관 정보를 함께 조회한다")
    void findAllWithSentenceByClubId() {
        Book book = bookRepository.save(new Book(new BookTitle("댓글 문장 조회 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(
                chapter, 1, Collections.singletonList(new SentenceContent("조회 문장"))));
        Member member = memberRepository.save(new Member(new Nickname("조회 작성자")));
        Club club = clubRepository.save(new Club(new ClubName("조회 모임"), book, new JoinCode("FETCH1")));
        Club otherClub = clubRepository.save(new Club(new ClubName("다른 모임"), book, new JoinCode("FETCH2")));
        ClubMember membership = clubMemberRepository.save(new ClubMember(member, club));
        ClubMember otherMembership = clubMemberRepository.save(new ClubMember(member, otherClub));
        Comment firstExpected = commentRepository.save(new Comment(
                membership, passage.getSentences().getFirst(), new CommentContent("첫 조회 댓글")));
        Comment secondExpected = commentRepository.save(new Comment(
                membership, passage.getSentences().getFirst(), new CommentContent("둘째 조회 댓글")));
        commentRepository.save(new Comment(
                otherMembership, passage.getSentences().getFirst(), new CommentContent("다른 모임 댓글")));
        commentRepository.flush();
        entityManager.clear();

        var comments = commentRepository.findAllWithSentenceByClubId(club.getId());
        entityManager.clear();

        assertThat(comments).extracting(Comment::getId)
                .containsExactlyInAnyOrder(firstExpected.getId(), secondExpected.getId());
        assertThat(comments).allSatisfy(comment -> {
            assertThat(comment.getWriter().getNickname()).isEqualTo("조회 작성자");
            assertThat(comment.getSentence().getContent()).isEqualTo("조회 문장");
            assertThat(comment.getSentence().getPassage().getId()).isEqualTo(passage.getId());
            assertThat(comment.isPublicRoomComment()).isFalse();
        });
    }
}
