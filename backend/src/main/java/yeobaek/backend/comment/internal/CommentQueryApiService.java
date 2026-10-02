package yeobaek.backend.comment.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentQueryApi;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.LocationCommentCount;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryApiService implements CommentQueryApi {

    private final CommentRepository commentRepository;

    @Override
    public Map<ContentLocationId, Long> countByLocation(
            SpaceId spaceId, List<ContentLocationId> locationIds, Set<MemberId> excludedAuthorIds) {
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        List<LocationCommentCount> counts = excludedAuthorIds.isEmpty()
                ? commentRepository.countBySpaceIdAndLocationIdIn(spaceId.value(), locationValues(locationIds))
                : commentRepository.countExcludingAuthorsBySpaceIdAndLocationIdIn(
                        spaceId.value(), locationValues(locationIds),
                        excludedAuthorIds.stream().map(MemberId::value).toList());
        return counts.stream()
                .collect(Collectors.toUnmodifiableMap(
                        count -> new ContentLocationId(count.getLocationId()),
                        LocationCommentCount::getCommentCount));
    }

    private List<Long> locationValues(List<ContentLocationId> locationIds) {
        return locationIds.stream().map(ContentLocationId::value).toList();
    }
}
