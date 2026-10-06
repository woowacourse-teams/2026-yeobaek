package yeobaek.backend.web.v2.appreciation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.web.v2.appreciation.AppreciationRequests.CommentData;

@Component
@RequiredArgsConstructor
public class CommentAppreciationWebAdapter implements AppreciationWebAdapter {

    private final CommentQueryWorkflow queries;
    private final CommentSharingWorkflow sharing;
    private final CommentModificationWorkflow modifications;

    @Override
    public AppreciationKind kind() {
        return AppreciationKind.COMMENT;
    }

    @Override
    public List<AppreciationResponses.Appreciation> find(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                                          ContentLocationId locationId) {
        return queries.findComments(actorId, spaceId, contentId, locationId).stream()
                .map(result -> AppreciationResponses.from(result, actorId.value()))
                .toList();
    }

    @Override
    public AppreciationResponses.Appreciation create(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                                       ContentLocationId locationId,
                                                       AppreciationRequests.Data data) {
        CommentData comment = requireComment(data);
        return AppreciationResponses.from(sharing.share(actorId, spaceId, contentId, locationId, comment.content()),
                actorId.value());
    }

    @Override
    public AppreciationResponses.Appreciation update(MemberId actorId, AppreciationId appreciationId,
                                                       AppreciationRequests.Data data) {
        return AppreciationResponses.from(modifications.update(actorId, appreciationId, requireComment(data).content()),
                actorId.value());
    }

    private static CommentData requireComment(AppreciationRequests.Data data) {
        if (!(data instanceof CommentData comment)) {
            throw new IllegalArgumentException("감상 종류와 요청 data가 일치하지 않습니다.");
        }
        return comment;
    }
}
