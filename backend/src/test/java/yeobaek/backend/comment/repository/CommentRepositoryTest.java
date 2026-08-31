package yeobaek.backend.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.member.domain.Member;
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

    @Test
    @DisplayName("댓글을 저장하면 작성일이 자동으로 기록되고 수정일은 비어 있다")
    void saveSetsCreatedAt() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 1));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, "본문"));
        Member member = memberRepository.save(new Member("민서"));
        Club club = clubRepository.save(new Club("1기", book, "CODE03"));
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(member, club));

        Comment saved = commentRepository.saveAndFlush(
                new Comment(clubMember, passage.getSentences().getFirst(), "이 문장에서 멈칫했어요."));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.getClubMember().getMember().getNickname()).isEqualTo("민서");
    }

    @Test
    @DisplayName("조회할 문장 ID에 대해 현재 모임의 댓글 수만 집계한다")
    void countsCommentsByClubAndSentenceIds() {
        Book book = bookRepository.save(new Book("댓글 집계 도서", null, null, 1));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, java.util.List.of("첫 문장.", "둘째 문장.")));
        Member member = memberRepository.save(new Member("민서"));
        Club club = clubRepository.save(new Club("1기", book, "COUNT1"));
        Club otherClub = clubRepository.save(new Club("2기", book, "COUNT2"));
        ClubMember membership = clubMemberRepository.save(new ClubMember(member, club));
        ClubMember otherMembership = clubMemberRepository.save(new ClubMember(member, otherClub));
        var firstSentence = passage.getSentences().getFirst();
        var secondSentence = passage.getSentences().get(1);
        commentRepository.save(new Comment(membership, firstSentence, "우리 모임 댓글 1"));
        commentRepository.save(new Comment(membership, firstSentence, "우리 모임 댓글 2"));
        commentRepository.save(new Comment(otherMembership, firstSentence, "다른 모임 댓글"));

        var counts = commentRepository.countByClubIdAndSentenceIdIn(
                club.getId(), java.util.List.of(firstSentence.getId(), secondSentence.getId()));

        assertThat(counts).singleElement()
                .satisfies(count -> {
                    assertThat(count.getSentenceId()).isEqualTo(firstSentence.getId());
                    assertThat(count.getCommentCount()).isEqualTo(2);
                });
    }
}
