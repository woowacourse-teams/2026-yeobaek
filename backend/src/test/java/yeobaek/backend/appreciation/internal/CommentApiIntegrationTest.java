package yeobaek.backend.appreciation.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.appreciation.persistence.AppreciationRepository;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.IntegrationTest;

class CommentApiIntegrationTest extends IntegrationTest {

    @Autowired
    private CommentApi commentApi;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AppreciationRepository appreciationRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentViewRepository viewRepository;

    @Autowired
    private CommentReportRepository reportRepository;

    @Test
    void directCommandsPersistRootBodyUpdateAndCleanup() {
        Member author = memberRepository.save(new Member(new Nickname("작성자")));
        Member reader = memberRepository.save(new Member(new Nickname("독자")));
        var created = commentApi.create(new MemberId(author.getId()), "원문");

        var updated = commentApi.update(new MemberId(author.getId()), created.id(), "수정문");
        commentApi.markViewed(new MemberId(reader.getId()), List.of(created.id()));

        assertPersistedUpdate(created.id().value(), updated);

        commentApi.delete(new MemberId(author.getId()), created.id());

        assertThat(List.of(commentRepository.count(), appreciationRepository.count(), viewRepository.count()))
                .containsOnly(0L);
    }

    private void assertPersistedUpdate(Long commentId, yeobaek.backend.appreciation.domain.Comment updated) {
        assertThat(updated.content()).isEqualTo("수정문");
        assertThat(updated.updatedAt()).isNotNull();
        assertThat(commentRepository.findById(commentId).orElseThrow().getContent()).isEqualTo("수정문");
        assertThat(appreciationRepository.findById(commentId).orElseThrow().getUpdatedAt()).isNotNull();
        assertThat(viewRepository.count()).isOne();
    }

    @Test
    void directReportIsSelfProtectedAndIdempotent() {
        Member author = memberRepository.save(new Member(new Nickname("작성자")));
        Member reporter = memberRepository.save(new Member(new Nickname("신고자")));
        var created = commentApi.create(new MemberId(author.getId()), "신고 대상");

        assertThatThrownBy(() -> commentApi.report(new MemberId(author.getId()), created.id()))
                .isInstanceOfSatisfying(CommentFailure.class,
                        failure -> assertThat(failure.reason())
                                .isEqualTo(CommentFailure.Reason.CANNOT_REPORT_OWN_COMMENT));
        assertThat(commentApi.report(new MemberId(reporter.getId()), created.id())).isTrue();
        assertThat(commentApi.report(new MemberId(reporter.getId()), created.id())).isFalse();
        assertThat(reportRepository.count()).isOne();
    }
}
