package yeobaek.backend.club.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.ClubMemberStatus;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.readmodel.book.SpaceBookReadModel;

class ClubMappingTest extends IntegrationTest {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Autowired
    private SpaceBookReadModel bookReadModel;

    @Autowired
    private ReadingProgressApi readingApi;

    private Book saveBook() {
        return bookRepository.save(newBook(new BookTitle("운수 좋은 날"), null, 1924, 10, null));
    }

    @Test
    @DisplayName("모임을 저장하면 도서와 참여 코드가 함께 조회된다")
    void saveAndFind() {
        Book book = saveBook();
        Club saved = clubRepository.save(newClub(new ClubName("교환독서 1기"), new JoinCode("A3F9KQ")));
        bindingApi.bind(new SpaceId(saved.getSpaceId()), new ContentId(book.getContentId()));

        transactionTemplate.executeWithoutResult(status -> {
            Club found = clubRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getJoinCode()).isEqualTo("A3F9KQ");
            assertThat(bookReadModel.findBook(new SpaceId(found.getSpaceId())).orElseThrow().title())
                    .isEqualTo("운수 좋은 날");
        });
    }

    @Test
    @DisplayName("참여 코드가 중복되면 저장에 실패한다")
    void duplicateJoinCodeRejected() {
        clubRepository.saveAndFlush(newClub(new ClubName("1기"), new JoinCode("SAME01")));

        assertThatThrownBy(() -> clubRepository.saveAndFlush(newClub(new ClubName("2기"), new JoinCode("SAME01"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 회원이 같은 모임에 두 번 참여하면 저장에 실패한다")
    void duplicateParticipationRejected() {
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE01")));
        clubMemberRepository.saveAndFlush(new ClubMember(new MemberId(member.getId()), club));

        assertThatThrownBy(() -> clubMemberRepository.saveAndFlush(new ClubMember(new MemberId(member.getId()), club)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("모임 참여 직후에는 최근 열람 본문과 마지막 읽은 시간이 비어 있다")
    void progressStartsEmpty() {
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Book book = saveBook();
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE02")));

        bindingApi.bind(new SpaceId(club.getSpaceId()), new ContentId(book.getContentId()));
        clubMemberRepository.save(new ClubMember(new MemberId(member.getId()), club));

        assertThat(readingApi.find(new MemberId(member.getId()), new SpaceId(club.getSpaceId()),
                new ContentId(book.getContentId()))).isEmpty();
    }

    @Test
    @DisplayName("모임을 탈퇴하면 탈퇴 상태가 저장되고 조회된다")
    void leaveStatusPersists() {
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE03")));
        ClubMember membership = clubMemberRepository.save(new ClubMember(new MemberId(member.getId()), club));
        membership.leave();
        clubMemberRepository.saveAndFlush(membership);

        transactionTemplate.executeWithoutResult(status -> {
            ClubMember found = clubMemberRepository.findById(membership.getId()).orElseThrow();

            assertThat(found.getStatus()).isEqualTo(ClubMemberStatus.LEFT);
        });
    }

    @Test
    @DisplayName("상태를 지정하지 않은 기존 참여 행은 DB 기본값으로 참여 중 상태가 된다")
    void statusDefaultsToJoined() {
        Member member = memberRepository.save(new Member(new Nickname("민서")));
        Club club = clubRepository.save(newClub(new ClubName("1기"), new JoinCode("CODE04")));

        jdbcTemplate.update("insert into club_members (member_id, club_id) values (?, ?)",
                member.getId(), club.getId());

        String status = jdbcTemplate.queryForObject(
                "select status from club_members where member_id = ? and club_id = ?",
                String.class, member.getId(), club.getId());
        assertThat(status).isEqualTo("JOINED");
    }
}
