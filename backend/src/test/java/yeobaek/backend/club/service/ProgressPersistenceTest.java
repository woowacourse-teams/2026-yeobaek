package yeobaek.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.DatabaseCleaningIntegrationTest;

class ProgressPersistenceTest extends DatabaseCleaningIntegrationTest {

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
    private MemberRepository memberRepository;
    @Autowired
    private ProgressService progressService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("읽기 위치를 다시 저장해도 진도는 한 건이며 나중에 요청한 본문을 가리킨다")
    void repeatedWritesLeaveLatestProgress() {
        Book book = bookRepository.save(new Book(new BookTitle("이어 읽는 책"), null, null, 2,
                null, null, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("첫 장"), 1));
        Passage first = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("앞부분"))));
        Passage second = passageRepository.save(new Passage(chapter, 2, List.of(new SentenceContent("뒷부분"))));
        Club club = clubRepository.save(new Club(new ClubName("독서 모임"), book, new JoinCode("WRITE1")));
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        ClubMember membership = clubMemberRepository.save(new ClubMember(reader, club));

        progressService.updateProgress(reader.getId(), club.getId(), second.getId());
        progressService.updateProgress(reader.getId(), club.getId(), first.getId());

        assertThat(jdbcTemplate.queryForList("""
                select last_read_passage_id from club_member_last_reads where club_member_id = ?
                """, Long.class, membership.getId())).containsExactly(first.getId());
    }
}
