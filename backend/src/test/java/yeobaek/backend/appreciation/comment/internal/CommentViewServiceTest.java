package yeobaek.backend.appreciation.comment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.appreciation.comment.repository.CommentViewRepository;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

class CommentViewServiceTest {

    private final CommentRepository comments = mock(CommentRepository.class);
    private final CommentViewRepository views = mock(CommentViewRepository.class);
    private final CommentViewService service = new CommentViewService(comments, views);

    @Test
    void markViewedSavesOnlyExistingCommentsThatHaveNotAlreadyBeenViewed() {
        var actorId = new MemberId(1L);
        var first = new AppreciationId(10L);
        var alreadyViewed = new AppreciationId(11L);
        var missing = new AppreciationId(12L);
        given(views.findViewedCommentIds(1L, List.of(10L, 11L, 12L))).willReturn(List.of(11L));
        given(comments.findAllById(List.of(10L, 11L, 12L))).willReturn(List.of(
                new Comment(10L, new CommentContent("새 조회")),
                new Comment(11L, new CommentContent("이미 조회"))));

        service.markViewed(actorId, List.of(first, alreadyViewed, missing));

        verify(views).saveAll(argThat(saved -> {
            var found = new java.util.ArrayList<yeobaek.backend.appreciation.comment.domain.CommentView>();
            saved.forEach(found::add);
            return found.size() == 1
                    && found.getFirst().getActorId().equals(1L)
                    && found.getFirst().getAppreciationId().equals(10L);
        }));
    }

    @Test
    void emptyInputsDoNotAccessRepositories() {
        service.markViewed(new MemberId(1L), List.of());
        assertThat(service.findViewedIds(new MemberId(1L), List.of())).isEmpty();

        verifyNoInteractions(comments, views);
    }
}
