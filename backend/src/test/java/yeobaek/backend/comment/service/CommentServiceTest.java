package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.CommentView;
import yeobaek.backend.comment.domain.ContentVisibility;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class CommentServiceTest extends IntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private CommentViewRepository commentViewRepository;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    private Member writer;
    private Book book;
    private Member other;
    private Club club;
    private Club otherClub;
    private Passage passage;
    private Sentence sentence;

    @BeforeEach
    void setUp() {
        book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 2));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        passage = passageRepository.save(new Passage(chapter, 1, "본문 1"));
        sentence = passage.getSentences().getFirst();
        writer = memberRepository.save(new Member("민서"));
        other = memberRepository.save(new Member("지수"));
        club = clubRepository.save(new Club("1기", book, "CODE01"));
        otherClub = clubRepository.save(new Club("2기", book, "CODE02"));
        clubMemberRepository.save(new ClubMember(writer, club));
        clubMemberRepository.save(new ClubMember(other, club));
    }

    @Test
    @DisplayName("댓글을 작성하면 작성자 정보와 mine 플래그가 함께 응답된다")
    void createComment() {
        CommentResponse response = commentService.create(writer.getId(), club.getId(), sentence.getId(), "멈칫했어요.");

        assertThat(response.nickname()).isEqualTo("민서");
        assertThat(response.mine()).isTrue();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNull();
        assertThat(commentRepository.count()).isEqualTo(1);

    }

    @Test
    @DisplayName("기능 도입 전에 존재한 본인 댓글은 조회 기록이 없어 NEW로 계산된다")
    void treatExistingOwnCommentAsNew() {
        ClubMember membership = clubMemberRepository.findByMemberIdAndClubId(writer.getId(), club.getId()).orElseThrow();
        commentRepository.save(new Comment(membership, sentence, "기존 댓글"));

        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 문장 목록 조회는 조회 상태와 저장된 진도를 변경하지 않는다")
    void discoveryListDoesNotChangeViewStatusOrProgress() {
        commentService.create(other.getId(), club.getId(), sentence.getId(), "새 댓글");

        var response = commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId());

        assertThat(response.commentedSentences()).singleElement().satisfies(item -> {
            assertThat(item.sentenceId()).isEqualTo(sentence.getId());
            assertThat(item.unreadCommentCount()).isEqualTo(1);
            assertThat(item.contentVisibility()).isEqualTo(ContentVisibility.VISIBLE);
        });
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(1);
        assertThat(clubMemberRepository.findByMemberIdAndClubId(writer.getId(), club.getId()).orElseThrow()
                .getLastReadPassage()).isNull();
    }

    @Test
    @DisplayName("중복 조회 기록이 있어도 댓글 수와 미확인 댓글 수를 중복 집계하지 않는다")
    void aggregateSafelyWithDuplicateViews() {
        CommentResponse created = commentService.create(other.getId(), club.getId(), sentence.getId(), "새 댓글");
        Comment comment = commentRepository.findById(created.commentId()).orElseThrow();
        commentViewRepository.save(new CommentView(writer, comment));
        commentViewRepository.save(new CommentView(writer, comment));

        var item = commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId())
                .commentedSentences().getFirst();

        assertThat(item.commentCount()).isEqualTo(1);
        assertThat(item.unreadCommentCount()).isZero();
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isZero();
    }

    @Test
    @DisplayName("상세 조회는 보이는 댓글만 VIEWED로 전환하고 차단한 작성자의 댓글은 제외한다")
    void viewOnlyVisibleCommentDetails() {
        CommentResponse visible = commentService.create(other.getId(), club.getId(), sentence.getId(), "보이는 댓글");
        Member blocked = memberRepository.save(new Member("차단 대상"));
        clubMemberRepository.save(new ClubMember(blocked, club));
        CommentResponse hidden = commentService.create(blocked.getId(), club.getId(), sentence.getId(), "숨긴 댓글");
        memberBlockRepository.save(new MemberBlock(writer, blocked));

        CommentsResponse response = commentService.findComments(writer.getId(), club.getId(), sentence.getId());

        assertThat(response.comments()).extracting(CommentResponse::commentId).containsExactly(visible.commentId());
        assertThat(commentViewRepository.existsByMemberIdAndCommentId(writer.getId(), visible.commentId())).isTrue();
        assertThat(commentViewRepository.existsByMemberIdAndCommentId(writer.getId(), hidden.commentId())).isFalse();
    }

    @Test
    @DisplayName("댓글 문장은 현재 미확인, 미래 미확인, 모두 확인 그룹 순서로 정렬된다")
    void sortCommentedSentencesByDiscoveryGroups() {
        Chapter chapter = chapterRepository.save(new Chapter(book, "2장", 2));
        Passage futurePassage = passageRepository.save(new Passage(chapter, 2, "미래 문장"));
        Passage viewedPassage = passageRepository.save(new Passage(chapter, 3, "확인한 문장"));
        commentService.create(other.getId(), club.getId(), sentence.getId(), "현재 새 댓글");
        commentService.create(other.getId(), club.getId(), futurePassage.getSentences().getFirst().getId(), "미래 새 댓글");
        commentService.create(writer.getId(), club.getId(), viewedPassage.getSentences().getFirst().getId(), "확인한 댓글");

        var response = commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId());

        assertThat(response.commentedSentences())
                .extracting(item -> item.content())
                .containsExactly("본문 1", "미래 문장", "확인한 문장");
        assertThat(response.commentedSentences().get(1).contentVisibility())
                .isEqualTo(ContentVisibility.REVEAL_REQUIRED);
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 목록은 작성일 오름차순이고 본인 여부가 표시되며 같은 모임의 댓글만 보인다")
    void findCommentsOrderedAndIsolated() {
        commentService.create(writer.getId(), club.getId(), sentence.getId(), "첫 댓글");
        commentService.create(other.getId(), club.getId(), sentence.getId(), "둘째 댓글");
        ClubMember otherClubMembership = clubMemberRepository.save(new ClubMember(writer, otherClub));
        commentRepository.save(new Comment(otherClubMembership, sentence, "다른 모임 댓글"));

        CommentsResponse response = commentService.findComments(writer.getId(), club.getId(), sentence.getId());

        assertThat(response.comments()).hasSize(2);
        assertThat(response.comments().get(0).content()).isEqualTo("첫 댓글");
        assertThat(response.comments().get(0).mine()).isTrue();
        assertThat(response.comments().get(1).nickname()).isEqualTo("지수");
        assertThat(response.comments().get(1).mine()).isFalse();
    }

    @Test
    @DisplayName("요청자가 차단한 회원의 댓글만 단방향으로 숨긴다")
    void excludeBlockedWritersCommentsDirectionally() {
        commentService.create(writer.getId(), club.getId(), sentence.getId(), "작성자 댓글");
        commentService.create(other.getId(), club.getId(), sentence.getId(), "차단자 댓글");
        memberBlockRepository.save(new MemberBlock(writer, other));

        CommentsResponse blockerResponse = commentService.findComments(writer.getId(), club.getId(), sentence.getId());
        CommentsResponse blockedResponse = commentService.findComments(other.getId(), club.getId(), sentence.getId());

        assertThat(blockerResponse.comments()).extracting(CommentResponse::content)
                .containsExactly("작성자 댓글");
        assertThat(blockedResponse.comments()).extracting(CommentResponse::content)
                .containsExactly("작성자 댓글", "차단자 댓글");
    }

    @Test
    @DisplayName("탈퇴 회원의 댓글은 작성자 정보와 내용이 그대로 조회된다")
    void preserveLeftMembersComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "남겨진 댓글");
        leaveClub(writer, club);

        CommentsResponse response = commentService.findComments(other.getId(), club.getId(), sentence.getId());

        assertThat(response.comments()).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(created.commentId());
            assertThat(comment.nickname()).isEqualTo("민서");
            assertThat(comment.content()).isEqualTo("남겨진 댓글");
            assertThat(comment.mine()).isFalse();
        });
    }

    @Test
    @DisplayName("모임 미소속 회원은 댓글을 작성할 수 없다")
    void rejectOutsider() {
        Member outsider = memberRepository.save(new Member("외부인"));

        assertThatThrownBy(() -> commentService.create(outsider.getId(), club.getId(), sentence.getId(), "댓글"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("탈퇴 회원은 댓글을 조회하거나 작성할 수 없다")
    void rejectCommentContextForLeftMember() {
        leaveClub(writer, club);

        assertThatThrownBy(() -> commentService.findComments(writer.getId(), club.getId(), sentence.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> commentService.create(writer.getId(), club.getId(), sentence.getId(), "댓글"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("본인 댓글을 수정하면 내용이 바뀌고 수정일이 기록된다")
    void updateOwnComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "원본");

        CommentResponse response = commentService.update(writer.getId(), created.commentId(), "수정된 내용");

        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("남의 댓글은 수정할 수 없다")
    void rejectUpdatingOthersComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "원본");

        assertThatThrownBy(() -> commentService.update(other.getId(), created.commentId(), "탈취 시도"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("본인 댓글을 삭제하면 하드 삭제된다")
    void deleteOwnComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "삭제될 댓글");

        commentService.delete(writer.getId(), created.commentId());

        assertThat(commentRepository.findById(created.commentId())).isEmpty();
    }

    @Test
    @DisplayName("남의 댓글은 삭제할 수 없다")
    void rejectDeletingOthersComment() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "원본");

        assertThatThrownBy(() -> commentService.delete(other.getId(), created.commentId()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(commentRepository.findById(created.commentId())).isPresent();
    }

    @Test
    @DisplayName("탈퇴 회원은 재가입 전까지 기존 댓글을 수정하거나 삭제할 수 없다")
    void rejectChangingCommentForLeftMember() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "보존할 댓글");
        leaveClub(writer, club);

        assertThatThrownBy(() -> commentService.update(writer.getId(), created.commentId(), "수정 시도"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> commentService.delete(writer.getId(), created.commentId()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(commentRepository.findById(created.commentId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 댓글의 수정·삭제는 실패한다")
    void rejectUnknownComment() {
        assertThatThrownBy(() -> commentService.update(writer.getId(), 999L, "내용"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commentService.delete(writer.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("같은 댓글을 다시 신고해도 신고 한 건만 저장된다")
    void reportCommentIdempotently() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "신고 대상");

        commentService.report(other.getId(), created.commentId());
        commentService.report(other.getId(), created.commentId());

        assertThat(commentReportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 신고 후에도 해당 댓글은 목록에 노출된다")
    void preserveCommentExposureAfterReport() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "신고 대상");

        commentService.report(other.getId(), created.commentId());

        assertThat(commentService.findComments(other.getId(), club.getId(), sentence.getId()).comments())
                .singleElement()
                .extracting(CommentResponse::commentId)
                .isEqualTo(created.commentId());
    }

    @Test
    @DisplayName("본인 댓글은 신고할 수 없다")
    void rejectOwnCommentReport() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "본인 댓글");

        assertThatThrownBy(() -> commentService.report(writer.getId(), created.commentId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.CANNOT_REPORT_OWN_COMMENT);
    }

    @Test
    @DisplayName("존재하지 않는 댓글은 신고할 수 없다")
    void rejectUnknownCommentReport() {
        assertThatThrownBy(() -> commentService.report(other.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("신고자가 차단한 작성자의 댓글은 보이지 않는 댓글로 처리한다")
    void rejectBlockedWriterCommentReport() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "차단된 댓글");
        memberBlockRepository.save(new MemberBlock(other, writer));

        assertThatThrownBy(() -> commentService.report(other.getId(), created.commentId()))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("모임에 참여하지 않은 회원은 댓글을 신고할 수 없다")
    void rejectOutsiderCommentReport() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "신고 대상");
        Member outsider = memberRepository.save(new Member("외부인"));

        assertThatThrownBy(() -> commentService.report(outsider.getId(), created.commentId()))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
    }

    @Test
    @DisplayName("모임을 탈퇴한 회원은 댓글을 신고할 수 없다")
    void rejectLeftMemberCommentReport() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "신고 대상");
        leaveClub(other, club);

        assertThatThrownBy(() -> commentService.report(other.getId(), created.commentId()))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
    }

    @Test
    @DisplayName("존재하지 않는 문장의 댓글 조회는 SENTENCE_NOT_FOUND로 실패한다")
    void rejectUnknownSentence() {
        assertThatThrownBy(() -> commentService.findComments(writer.getId(), club.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.SENTENCE_NOT_FOUND);
    }

    @Test
    @DisplayName("모임의 도서에 속하지 않는 문장에는 댓글을 달 수 없다")
    void rejectPassageOfOtherBook() {
        Book otherBook = bookRepository.save(new Book("다른 책", null, null, 1));
        Chapter otherChapter = chapterRepository.save(new Chapter(otherBook, "1장", 1));
        Passage otherPassage = passageRepository.save(new Passage(otherChapter, 1, "다른 본문"));

        assertThatThrownBy(() -> commentService.create(writer.getId(), club.getId(),
                otherPassage.getSentences().getFirst().getId(), "댓글"))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.SENTENCE_NOT_FOUND);
    }

    @Nested
    @DisplayName("삭제된 도서를 대상으로 독서 활동을 할 수 있는가")
    class CommentActivityOfDeletedBook {

        private CommentResponse existingComment;

        @BeforeEach
        void deleteBookAfterWritingComment() {
            existingComment = commentService.create(writer.getId(), club.getId(), sentence.getId(), "원본");
            bookRepository.delete(book.getId());
        }

        @Test
        @DisplayName("기존 댓글을 조회할 수 없다")
        void cannotReadComments() {
            assertBookNotAvailable(() ->
                    commentService.findComments(writer.getId(), club.getId(), sentence.getId()));
        }

        @Test
        @DisplayName("새 댓글을 작성할 수 없다")
        void cannotCreateComment() {
            assertBookNotAvailable(() ->
                    commentService.create(writer.getId(), club.getId(), sentence.getId(), "신규"));
            assertThat(commentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("기존 댓글을 수정할 수 없고 내용은 보존된다")
        void cannotUpdateCommentAndPreservesContent() {
            assertBookNotAvailable(() ->
                    commentService.update(writer.getId(), existingComment.commentId(), "수정"));
            assertThat(commentRepository.findById(existingComment.commentId())).get()
                    .extracting(Comment::getContent).isEqualTo("원본");
        }

        @Test
        @DisplayName("기존 댓글을 삭제할 수 없고 데이터는 보존된다")
        void cannotDeleteCommentAndPreservesData() {
            assertBookNotAvailable(() -> commentService.delete(writer.getId(), existingComment.commentId()));
            assertThat(commentRepository.findById(existingComment.commentId())).isPresent();
        }

        @Test
        @DisplayName("기존 댓글을 신고할 수 없고 신고 데이터는 생성되지 않는다")
        void cannotReportComment() {
            assertBookNotAvailable(() -> commentService.report(other.getId(), existingComment.commentId()));
            assertThat(commentReportRepository.count()).isZero();
        }
    }

    @Autowired
    private yeobaek.backend.member.service.MemberService memberService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("본인 작성은 즉시 확인되고 반복 상세 조회는 기록을 추가하지 않는다")
    void ownCommentAndRepeatedViews() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "내 댓글");
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isZero();
        commentService.findComments(other.getId(), club.getId(), sentence.getId());
        commentService.findComments(other.getId(), club.getId(), sentence.getId());
        assertThat(commentViewRepository.count()).isEqualTo(2);
        commentService.update(writer.getId(), created.commentId(), "수정 후에도 확인 유지");
        assertThat(commentService.countNewComments(other.getId(), club.getId(), passage.getId()).newCommentCount())
                .isZero();
    }

    @Test
    @DisplayName("요청 진도 경계와 차단을 적용하고 차단 해제 뒤 새 조회에서 갱신한다")
    void countWithinRequestedProgressAndVisibility() {
        Chapter chapter = chapterRepository.save(new Chapter(book, "미래 장", 2));
        Passage future = passageRepository.save(new Passage(chapter, 2, "미래 본문"));
        commentService.create(other.getId(), club.getId(), sentence.getId(), "현재");
        commentService.create(other.getId(), club.getId(), future.getSentences().getFirst().getId(), "미래");
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(1);
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), future.getId()).newCommentCount())
                .isEqualTo(2);
        MemberBlock block = memberBlockRepository.save(new MemberBlock(writer, other));
        assertThat(commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId())
                .commentedSentences()).isEmpty();
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), future.getId()).newCommentCount())
                .isZero();
        memberBlockRepository.delete(block);
        assertThat(commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId())
                .commentedSentences()).hasSize(2);
    }

    @Test
    @DisplayName("탈퇴 재가입 시 확인 상태를 유지하고 탈퇴 중 댓글과 신규 가입 전 댓글은 NEW다")
    void retainViewsAcrossRejoin() {
        commentService.create(other.getId(), club.getId(), sentence.getId(), "기존");
        commentService.findComments(writer.getId(), club.getId(), sentence.getId());
        leaveClub(writer, club);
        commentService.create(other.getId(), club.getId(), sentence.getId(), "탈퇴 중");
        ClubMember membership = clubMemberRepository.findByMemberIdAndClubId(writer.getId(), club.getId()).orElseThrow();
        membership.rejoin();
        clubMemberRepository.save(membership);
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(1);
        Member newcomer = memberRepository.save(new Member("신규 참여자"));
        clubMemberRepository.save(new ClubMember(newcomer, club));
        assertThat(commentService.countNewComments(newcomer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("댓글 삭제는 중복 확인 기록까지 삭제하고 목록에서 제외한다")
    void cascadeCommentViews() {
        CommentResponse created = commentService.create(writer.getId(), club.getId(), sentence.getId(), "삭제 대상");
        Comment comment = commentRepository.findById(created.commentId()).orElseThrow();
        commentViewRepository.save(new CommentView(other, comment));
        commentViewRepository.save(new CommentView(other, comment));
        commentService.delete(writer.getId(), created.commentId());
        assertThat(commentViewRepository.count()).isZero();
        assertThat(commentService.findCommentedSentences(other.getId(), club.getId(), passage.getId())
                .commentedSentences()).isEmpty();
    }

    @Test
    @DisplayName("회원 삭제는 본인의 기록과 본인 댓글에 대한 타인의 기록을 함께 삭제한다")
    void cascadeMemberViews() {
        commentService.create(writer.getId(), club.getId(), sentence.getId(), "작성자 댓글");
        commentService.create(other.getId(), club.getId(), sentence.getId(), "남길 댓글");
        commentService.findComments(writer.getId(), club.getId(), sentence.getId());
        commentService.findComments(other.getId(), club.getId(), sentence.getId());
        memberService.delete(writer.getId());
        assertThat(commentViewRepository.count()).isEqualTo(1);
        assertThat(commentRepository.count()).isEqualTo(1);
        assertThat(commentService.findCommentedSentences(other.getId(), club.getId(), passage.getId())
                .commentedSentences().getFirst().commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 그룹은 최신 댓글 시각으로 정렬하고 동점은 문장 ID 내림차순이다")
    void sortLatestCommentAndSentenceTie() {
        Chapter chapter = chapterRepository.save(new Chapter(book, "정렬 장", 2));
        Passage otherPassage = passageRepository.save(new Passage(chapter, 1, "다른 문장"));
        Long secondSentenceId = otherPassage.getSentences().getFirst().getId();
        CommentResponse first = commentService.create(other.getId(), club.getId(), sentence.getId(), "첫 문장");
        commentService.create(other.getId(), club.getId(), secondSentenceId, "둘째 문장");
        jdbcTemplate.update("update comments set created_at = ?", java.sql.Timestamp.valueOf("2026-09-07 12:00:00"));
        assertThat(commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId())
                .commentedSentences()).extracting(item -> item.sentenceId())
                .containsExactly(secondSentenceId, sentence.getId());
        jdbcTemplate.update("update comments set created_at = ? where id = ?",
                java.sql.Timestamp.valueOf("2026-09-07 13:00:00"), first.commentId());
        assertThat(commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId())
                .commentedSentences()).extracting(item -> item.sentenceId())
                .containsExactly(sentence.getId(), secondSentenceId);
    }

    @Test
    @DisplayName("상세 조회 트랜잭션 실패 시 확인 기록도 롤백된다")
    void rollBackDetailViews() {
        commentService.create(other.getId(), club.getId(), sentence.getId(), "롤백 대상");
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            commentService.findComments(writer.getId(), club.getId(), sentence.getId());
            throw new IllegalStateException("응답 생성 후 트랜잭션 실패");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(commentService.countNewComments(writer.getId(), club.getId(), passage.getId()).newCommentCount())
                .isEqualTo(1);
        assertThat(commentViewRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않거나 다른 책의 현재 문단은 INVALID_REQUEST다")
    void rejectInvalidDiscoveryPassage() {
        Book anotherBook = bookRepository.save(new Book("새 책", null, null, 1));
        Chapter chapter = chapterRepository.save(new Chapter(anotherBook, "새 장", 1));
        Passage foreign = passageRepository.save(new Passage(chapter, 1, "다른 책 본문"));
        for (Long passageId : java.util.List.of(Long.MAX_VALUE, foreign.getId())) {
            assertThatThrownBy(() -> commentService.countNewComments(writer.getId(), club.getId(), passageId))
                    .isInstanceOf(BadRequestException.class).extracting("code").isEqualTo(ErrorCode.INVALID_REQUEST);
            assertThatThrownBy(() -> commentService.findCommentedSentences(writer.getId(), club.getId(), passageId))
                    .isInstanceOf(BadRequestException.class).extracting("code").isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Test
    @DisplayName("발견 API는 없는 모임과 미소속 회원을 거부한다")
    void validateDiscoveryMembership() {
        assertThatThrownBy(() -> commentService.countNewComments(writer.getId(), Long.MAX_VALUE, passage.getId()))
                .isInstanceOf(NotFoundException.class).extracting("code").isEqualTo(ErrorCode.CLUB_NOT_FOUND);
        assertThatThrownBy(() -> commentService.findCommentedSentences(other.getId(), otherClub.getId(), passage.getId()))
                .isInstanceOf(ForbiddenException.class).extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
        leaveClub(writer, club);
        assertThatThrownBy(() -> commentService.countNewComments(writer.getId(), club.getId(), passage.getId()))
                .isInstanceOf(ForbiddenException.class).extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
    }

    @Test
    @DisplayName("삭제된 도서의 발견 API는 데이터를 노출하지 않는다")
    void rejectDeletedBookDiscovery() {
        commentService.create(writer.getId(), club.getId(), sentence.getId(), "보존 댓글");
        bookRepository.delete(book.getId());
        assertBookNotAvailable(() -> commentService.countNewComments(writer.getId(), club.getId(), passage.getId()));
        assertBookNotAvailable(() -> commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId()));
    }
    private void assertBookNotAvailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    private void leaveClub(Member member, Club targetClub) {
        ClubMember membership = clubMemberRepository
                .findByMemberIdAndClubId(member.getId(), targetClub.getId()).orElseThrow();
        membership.leave();
        clubMemberRepository.saveAndFlush(membership);
    }
    @Test
    @DisplayName("기존 페이지 크기보다 많은 문장도 전부 반환한다")
    void returnEntireListBeyondFormerPageSize() {
        Chapter chapter = chapterRepository.save(new Chapter(book, "전체 목록", 2));
        var contents = java.util.stream.IntStream.range(0, 25).mapToObj(index -> "문장 " + index).toList();
        Passage manySentences = passageRepository.save(new Passage(chapter, 2, contents));
        ClubMember author = clubMemberRepository.findByMemberIdAndClubId(other.getId(), club.getId()).orElseThrow();
        commentRepository.saveAll(manySentences.getSentences().stream()
                .map(item -> new Comment(author, item, "댓글")).toList());
        assertThat(commentService.findCommentedSentences(writer.getId(), club.getId(), passage.getId())
                .commentedSentences()).hasSize(25);
    }
}
