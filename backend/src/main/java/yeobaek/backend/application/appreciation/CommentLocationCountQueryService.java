package yeobaek.backend.application.appreciation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi.LocatedAppreciation;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLocationCountQueryService {

    private final AppreciationContextApi contexts;
    private final CommentApi comments;

    public Map<ContentLocationId, Long> countByLocation(
            SpaceId spaceId, List<ContentLocationId> locationIds, Set<MemberId> excludedAuthorIds) {
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        Set<ContentLocationId> selected = Set.copyOf(locationIds);
        Map<AppreciationId, ContentLocationId> locations = contexts.findInSpace(spaceId).stream()
                .filter(context -> selected.contains(context.locationId()))
                .collect(Collectors.toMap(LocatedAppreciation::appreciationId, LocatedAppreciation::locationId));
        return comments.findByIds(locations.keySet()).stream()
                .filter(comment -> !excludedAuthorIds.contains(comment.authorId()))
                .collect(Collectors.groupingBy(comment -> locations.get(comment.id()), Collectors.counting()));
    }
}
