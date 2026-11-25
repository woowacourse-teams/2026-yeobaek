package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.domain.PublicRoomMember;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.support.DatabaseCleaningIntegrationTest;

class MemberDeletionPersistenceTest extends DatabaseCleaningIntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private PassageRepository passageRepository;
    @Autowired
    private PublicRoomRepository publicRoomRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PublicRoomMemberRepository publicRoomMemberRepository;
    @Autowired
    private MemberService memberService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("계정을 삭제하면 그 회원의 공개방 방문·진도만 사라진다")
    void deleteOnlyWithdrawnMembersPublicRoomActivity() {
        Book book = bookRepository.save(new Book(new BookTitle("독서 도서"), null, null, 2,
                null, null, null, null, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("첫 장"), 1));
        Passage first = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("첫 본문"))));
        Passage second = passageRepository.save(new Passage(chapter, 2, List.of(new SentenceContent("둘째 본문"))));
        PublicRoom room = publicRoomRepository.save(new PublicRoom(book));
        Member leaving = memberRepository.save(new Member(new Nickname("탈퇴 회원")));
        Member remaining = memberRepository.save(new Member(new Nickname("남은 회원")));
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        saveActivity(leaving, room, first, readAt);
        PublicRoomMember retained = saveActivity(remaining, room, second, readAt.plusHours(1));

        memberService.delete(leaving.getId());

        assertThat(publicRoomMemberRepository.findAll()).extracting(PublicRoomMember::getId)
                .containsExactly(retained.getId());
        assertThat(jdbcTemplate.queryForList("select public_room_member_id from public_room_member_last_reads",
                Long.class)).containsExactly(retained.getId());
        assertThat(publicRoomMemberRepository.findReadingsByMemberId(remaining.getId())).singleElement()
                .satisfies(activity -> assertThat(activity)
                        .extracting(result -> result.getLastReadPassage().getId(), PublicRoomMember::getLastReadAt,
                                PublicRoomMember::getLastVisitedAt)
                        .containsExactly(second.getId(), readAt.plusHours(1), readAt.plusHours(1)));
    }

    private PublicRoomMember saveActivity(Member member, PublicRoom room, Passage passage, LocalDateTime at) {
        PublicRoomMember activity = new PublicRoomMember(member, room);
        activity.visit(at);
        activity.updateProgress(passage, at);
        return publicRoomMemberRepository.save(activity);
    }
}
