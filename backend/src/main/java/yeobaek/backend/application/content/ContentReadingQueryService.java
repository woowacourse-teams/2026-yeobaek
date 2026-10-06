package yeobaek.backend.application.content;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.appreciation.CommentLocationCountQueryService;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.content.api.value.PassageRange;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.space.api.access.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class ContentReadingQueryService {

    private static final int MAX_RANGE_SIZE = 100;

    private final MemberQuery members;
    private final SpaceAccessApi spaces;
    private final SpaceContentBindingApi bindings;
    private final ContentApi contents;
    private final ContentBodyApi bodies;
    private final CommentLocationCountQueryService comments;
    private final MemberBlockApi blocks;

    @Transactional(readOnly = true)
    public BodyResult findBody(MemberId actorId, SpaceId spaceId, ContentId contentId, int from, int to) {
        members.getProfile(actorId);
        PassageRange range = new PassageRange(from, to);
        if (range.size() > MAX_RANGE_SIZE) {
            throw new ContentReadingPolicyException(ErrorCode.INVALID_REQUEST,
                    "본문은 한 번에 최대 " + MAX_RANGE_SIZE + "개까지 조회할 수 있습니다.",
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "from", Integer.toString(from), "to", Integer.toString(to),
                            "maxRangeSize", Integer.toString(MAX_RANGE_SIZE),
                            "reason", "RANGE_TOO_LARGE"));
        }
        if (!spaces.canAccess(actorId, spaceId)) {
            throw new ContentReadingPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                    "본문을 조회할 공간에 접근할 수 없습니다: spaceId=" + spaceId.value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "reason", "SPACE_ACCESS_DENIED"));
        }
        if (!bindings.isBound(spaceId, contentId)) {
            throw new ContentReadingPolicyException(ErrorCode.CONTENT_NOT_BOUND,
                    "본문을 조회할 공간에 연결되지 않은 컨텐츠입니다: spaceId=" + spaceId.value()
                            + ", contentId=" + contentId.value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "reason", "CONTENT_NOT_BOUND"));
        }
        contents.requireAvailable(contentId);

        List<ContentBodyApi.Passage> found = bodies.findPassages(contentId, range.from(), range.to());
        List<ContentLocationId> sentenceIds = found.stream()
                .flatMap(passage -> passage.sentences().stream())
                .map(ContentBodyApi.Sentence::locationId)
                .toList();
        Map<ContentLocationId, Long> counts = comments.countByLocation(spaceId, sentenceIds,
                blocks.findBlockedProfiles(actorId).keySet());
        return new BodyResult(found.stream().map(passage -> new PassageResult(
                passage.locationId(), passage.sequence(), passage.chapterId(), passage.sentences().stream()
                .map(sentence -> new SentenceResult(sentence.locationId(), sentence.sequence(), sentence.content(),
                        counts.getOrDefault(sentence.locationId(), 0L)))
                .toList())).toList());
    }

    public record BodyResult(List<PassageResult> passages) {

        public BodyResult {
            passages = List.copyOf(passages);
        }
    }

    public record PassageResult(ContentLocationId locationId, int sequence, long sectionId,
                                List<SentenceResult> sentences) {

        public PassageResult {
            sentences = List.copyOf(sentences);
        }
    }

    public record SentenceResult(ContentLocationId locationId, int sequence, String content, long commentCount) {
    }
}
