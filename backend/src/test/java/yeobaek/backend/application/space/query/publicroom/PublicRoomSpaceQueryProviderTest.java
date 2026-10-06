package yeobaek.backend.application.space.query.publicroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryResultFactory;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.space.api.publicroom.PublicRoomResponse;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitResponse;

class PublicRoomSpaceQueryProviderTest {

    private static final MemberId ACTOR = new MemberId(1L);
    private static final SpaceId ACTIVE_SPACE = new SpaceId(2L);
    private static final SpaceId UNBOUND_SPACE = new SpaceId(3L);
    private static final SpaceId DANGLING_SPACE = new SpaceId(4L);
    private static final SpaceId INACTIVE_SPACE = new SpaceId(5L);
    private static final ContentId ACTIVE_CONTENT = new ContentId(12L);
    private static final ContentId DANGLING_CONTENT = new ContentId(14L);
    private static final ContentId INACTIVE_CONTENT = new ContentId(15L);

    @Test
    void excludesUnboundMissingAndInactiveContentFromPublicAndVisitedLists() {
        Fixture fixture = fixture();
        List<PublicRoomResponse> rooms = List.of(
                room(ACTIVE_SPACE), room(UNBOUND_SPACE), room(DANGLING_SPACE), room(INACTIVE_SPACE));
        given(fixture.rooms.findAll()).willReturn(rooms);
        given(fixture.visits.countVisitors(List.of(
                ACTIVE_SPACE, UNBOUND_SPACE, DANGLING_SPACE, INACTIVE_SPACE))).willReturn(Map.of());
        given(fixture.readings.findBySpaces(ACTOR, List.of(
                ACTIVE_SPACE, UNBOUND_SPACE, DANGLING_SPACE, INACTIVE_SPACE))).willReturn(List.of());
        LocalDateTime visitedAt = LocalDateTime.of(2026, 10, 3, 12, 0);
        given(fixture.visits.findVisits(ACTOR)).willReturn(List.of(
                new PublicRoomVisitResponse(ACTOR, ACTIVE_SPACE, visitedAt),
                new PublicRoomVisitResponse(ACTOR, UNBOUND_SPACE, visitedAt),
                new PublicRoomVisitResponse(ACTOR, DANGLING_SPACE, visitedAt),
                new PublicRoomVisitResponse(ACTOR, INACTIVE_SPACE, visitedAt)));
        rooms.forEach(room -> given(fixture.rooms.findBySpaceId(room.id())).willReturn(Optional.of(room)));

        assertThat(fixture.provider.findPublic(ACTOR)).extracting(SpaceQueryResult::spaceId)
                .containsExactly(ACTIVE_SPACE);
        assertThat(fixture.provider.findMine(ACTOR)).extracting(SpaceQueryResult::spaceId)
                .containsExactly(ACTIVE_SPACE);
    }

    @Test
    void detailKeepsInactiveContentHistoryVisible() {
        Fixture fixture = fixture();
        PublicRoomResponse room = room(INACTIVE_SPACE);
        given(fixture.rooms.findBySpaceId(INACTIVE_SPACE)).willReturn(Optional.of(room));
        given(fixture.results.content(INACTIVE_CONTENT)).willReturn(card(INACTIVE_CONTENT, false));
        given(fixture.visits.findLastVisit(ACTOR, INACTIVE_SPACE)).willReturn(Optional.empty());

        var found = fixture.provider.findDetail(ACTOR, INACTIVE_SPACE);

        assertThat(((SpaceQueryResult.PublicRoomDetail) found.data()).content().available()).isFalse();
    }

    private Fixture fixture() {
        var rooms = mock(PublicRoomApi.class);
        var visits = mock(PublicRoomVisitApi.class);
        var bindings = mock(SpaceContentBindingApi.class);
        var contents = mock(ContentApi.class);
        var readings = mock(ReadingProgressApi.class);
        var members = mock(MemberQuery.class);
        var results = mock(SpaceQueryResultFactory.class);
        bind(bindings, ACTIVE_SPACE, ACTIVE_CONTENT);
        given(bindings.findContents(UNBOUND_SPACE)).willReturn(List.of());
        bind(bindings, DANGLING_SPACE, DANGLING_CONTENT);
        bind(bindings, INACTIVE_SPACE, INACTIVE_CONTENT);
        available(contents, ACTIVE_CONTENT, true);
        available(contents, DANGLING_CONTENT, true);
        available(contents, INACTIVE_CONTENT, false);
        given(results.content(ACTIVE_CONTENT)).willReturn(card(ACTIVE_CONTENT, true));
        given(results.content(DANGLING_CONTENT)).willThrow(new ContentNotFoundException(DANGLING_CONTENT,
                "연결된 컨텐츠가 존재하지 않습니다: contentId=" + DANGLING_CONTENT.value()));
        given(readings.find(ACTOR, ACTIVE_SPACE, ACTIVE_CONTENT)).willReturn(Optional.empty());
        return new Fixture(new PublicRoomSpaceQueryProvider(
                rooms, visits, bindings, contents, readings, members, results),
                rooms, visits, readings, results);
    }

    private void bind(SpaceContentBindingApi bindings, SpaceId spaceId, ContentId contentId) {
        given(bindings.findContents(spaceId)).willReturn(List.of(contentId));
    }

    private void available(ContentApi contents, ContentId contentId, boolean available) {
        Content content = mock(Content.class);
        given(content.available()).willReturn(available);
        given(contents.getContent(contentId)).willReturn(content);
    }

    private PublicRoomResponse room(SpaceId id) {
        PublicRoomResponse room = mock(PublicRoomResponse.class);
        given(room.id()).willReturn(id);
        return room;
    }

    private ContentCardResult card(ContentId contentId, boolean available) {
        return new ContentCardResult(contentId, ContentKind.BOOK, "책", List.of("작가"), "표지", 10, available);
    }

    private record Fixture(PublicRoomSpaceQueryProvider provider, PublicRoomApi rooms,
                           PublicRoomVisitApi visits, ReadingProgressApi readings,
                           SpaceQueryResultFactory results) {
    }
}
