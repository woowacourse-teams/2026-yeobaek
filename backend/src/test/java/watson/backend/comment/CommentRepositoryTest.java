package watson.backend.comment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import watson.backend.book.Book;
import watson.backend.book.BookRepository;
import watson.backend.book.Chapter;
import watson.backend.book.ChapterRepository;
import watson.backend.book.Passage;
import watson.backend.book.PassageRepository;
import watson.backend.club.Club;
import watson.backend.club.ClubMember;
import watson.backend.club.ClubMemberRepository;
import watson.backend.club.ClubRepository;
import watson.backend.member.Member;
import watson.backend.member.MemberRepository;
import watson.backend.support.RepositoryTest;

class CommentRepositoryTest extends RepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

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
        Passage passage = passageRepository.save(new Passage(chapter, 1, "본문", null));
        Member member = memberRepository.save(new Member("민서"));
        Club club = clubRepository.save(new Club("1기", book, "CODE03"));
        ClubMember clubMember = clubMemberRepository.save(new ClubMember(member, club));

        Comment saved = commentRepository.saveAndFlush(new Comment(clubMember, passage, "이 문장에서 멈칫했어요."));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.getClubMember().getMember().getNickname()).isEqualTo("민서");
    }
}
