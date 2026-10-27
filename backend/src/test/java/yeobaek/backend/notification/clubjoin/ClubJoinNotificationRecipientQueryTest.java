package yeobaek.backend.notification.clubjoin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class ClubJoinNotificationRecipientQueryTest extends IntegrationTest {
    @Autowired
    private ClubJoinNotificationRecipientQueryRepository recipients;
    @Autowired
    private BookManagementRepository books;
    @Autowired
    private ClubRepository clubs;
    @Autowired
    private ClubMemberRepository memberships;
    @Autowired
    private MemberRepository members;
    @Autowired
    private MemberBlockRepository blocks;

    private Club club;
    private Member joiningMember;

    @BeforeEach
    void setUp() {
        Book book = books.save(new Book(new BookTitle("모임 알림 도서"), null, null, 1, null));
        club = clubs.save(new Club(new ClubName("모임 알림"), book, new JoinCode("JOIN01")));
        joiningMember = addMember("입장자");
    }

    @Test
    @DisplayName("입장자·탈퇴자·비구성원·입장자를 차단한 회원은 제외하고 읽기 진도와 무관하게 조회해요")
    void selectJoinedMembersExcludingJoinerAndBlockers() {
        Member recipient = addMember("수신자");
        Member left = addMember("탈퇴자");
        var membership = memberships.findByMemberIdAndClubId(left.getId(), club.getId()).orElseThrow();
        membership.leave();
        memberships.save(membership);
        members.save(new Member(new Nickname("비구성원")));
        Member blocker = addMember("차단자");
        blocks.save(new MemberBlock(blocker, joiningMember));

        assertThat(recipients.findRecipientMemberIds(club.getId(), joiningMember.getId()))
                .containsExactly(recipient.getId());
    }

    @Test
    @DisplayName("입장자가 수신자만 차단한 경우에는 알림 대상에서 제외하지 않아요")
    void retainRecipientBlockedByJoiningMember() {
        Member recipient = addMember("수신자");
        blocks.save(new MemberBlock(joiningMember, recipient));

        assertThat(recipients.findRecipientMemberIds(club.getId(), joiningMember.getId()))
                .containsExactlyElementsOf(List.of(recipient.getId()));
    }

    private Member addMember(String nickname) {
        Member member = members.save(new Member(new Nickname(nickname)));
        memberships.save(new ClubMember(member, club));
        return member;
    }
}
