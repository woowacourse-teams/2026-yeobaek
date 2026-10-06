package yeobaek.backend.query.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceApi;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceNotFoundException;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.api.club.ClubApi;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClubStatisticsQueryService {

    private final ClubApi clubs;
    private final AppreciationContextApi contexts;
    private final CommentApi comments;
    private final ContentLegacyReferenceApi references;
    private final SpaceContentBindingApi bindings;
    private final SpaceAccessApi spaces;

    public Map<Long, Long> countComments(List<Long> clubIds) {
        Map<Long, Long> counts = new HashMap<>();
        for (Long clubId : clubIds) {
            clubs.findById(clubId).ifPresent(club -> {
                var ids = contexts.findInSpace(club.id()).stream().map(context -> context.appreciationId()).toList();
                long count = comments.findByIds(ids).size();
                if (count > 0) {
                    counts.put(clubId, count);
                }
            });
        }
        return Map.copyOf(counts);
    }

    public Map<Long, Long> countClubsByBookIds(List<Long> bookIds) {
        Map<Long, Long> counts = new HashMap<>();
        for (Long bookId : bookIds) {
            try {
                var contentId = references.resolve(ContentKind.BOOK, bookId);
                long count = bindings.findSpaces(contentId).stream()
                        .filter(spaceId -> SpaceKind.CLUB.equals(spaces.getSpace(spaceId).kind())).count();
                if (count > 0) {
                    counts.put(bookId, count);
                }
            } catch (ContentLegacyReferenceNotFoundException failure) {
                // 기존 통계에서 존재하지 않는 도서는 결과 행을 만들지 않는다.
                continue;
            }
        }
        return Map.copyOf(counts);
    }
}
