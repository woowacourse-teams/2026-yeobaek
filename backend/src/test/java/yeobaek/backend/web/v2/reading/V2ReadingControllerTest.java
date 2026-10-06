package yeobaek.backend.web.v2.reading;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.content.ContentReadingQueryService;
import yeobaek.backend.application.reading.ReadingActivityQueryService;
import yeobaek.backend.application.reading.ReadingActivityResult;
import yeobaek.backend.application.reading.ReadingProgressCommandService;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.web.v2.common.ContentCardResponse;
import yeobaek.backend.web.v2.content.ContentWebAdapterRegistry;
import yeobaek.backend.web.v2.space.SpaceWebAdapter;
import yeobaek.backend.web.v2.space.SpaceWebAdapterRegistry;

@WebMvcTest(V2ReadingController.class)
class V2ReadingControllerTest extends ControllerTest {

    @MockitoBean private ContentReadingQueryService bodies;
    @MockitoBean private ReadingProgressCommandService progress;
    @MockitoBean private ReadingActivityQueryService activities;
    @MockitoBean private ContentWebAdapterRegistry contentAdapters;
    @MockitoBean private SpaceWebAdapterRegistry spaceAdapters;
    @MockitoBean private SpaceWebAdapter spaceAdapter;

    @Test
    void findBody() throws Exception {
        givenValidMember(1L);
        var result = new ContentReadingQueryService.BodyResult(List.of(
                new ContentReadingQueryService.PassageResult(new ContentLocationId(4L), 2, 3L,
                        List.of(new ContentReadingQueryService.SentenceResult(new ContentLocationId(5L), 1,
                                "문장", 6)))));
        given(bodies.findBody(new MemberId(1L), new SpaceId(2L), new ContentId(3L), 1, 10))
                .willReturn(result);

        mockMvc.perform(get("/api/v2/spaces/2/contents/3/body").header("X-Member-Id", "1")
                        .param("from", "1").param("to", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.passages").isArray()).andExpect(jsonPath("$.passages.length()").value(1))
                .andExpect(jsonPath("$.passages[0].locationId").value(4))
                .andExpect(jsonPath("$.passages[0].locationKind").value("PASSAGE"))
                .andExpect(jsonPath("$.passages[0].sequence").value(2))
                .andExpect(jsonPath("$.passages[0].chapterId").value(3))
                .andExpect(jsonPath("$.passages[0].sentences").isArray())
                .andExpect(jsonPath("$.passages[0].sentences.length()").value(1))
                .andExpect(jsonPath("$.passages[0].sentences[0].locationId").value(5))
                .andExpect(jsonPath("$.passages[0].sentences[0].locationKind").value("SENTENCE"))
                .andExpect(jsonPath("$.passages[0].sentences[0].sequence").value(1))
                .andExpect(jsonPath("$.passages[0].sentences[0].content").value("문장"))
                .andExpect(jsonPath("$.passages[0].sentences[0].commentCount").value(6));
        verify(bodies).findBody(new MemberId(1L), new SpaceId(2L), new ContentId(3L), 1, 10);
    }

    @Test
    void updateProgress() throws Exception {
        givenValidMember(1L);
        var time = LocalDateTime.of(2026, 10, 3, 12, 0);
        given(progress.update(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L))).willReturn(new ReadingProgressCommandService.ProgressResult(8, 40, time));

        mockMvc.perform(put("/api/v2/spaces/2/contents/3/reading-progress").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lastReadLocationId\":4}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lastReadPassageSequence").value(8))
                .andExpect(jsonPath("$.progressRate").value(40))
                .andExpect(jsonPath("$.lastReadAt").value("2026-10-03T12:00:00"));
        verify(progress).update(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L));
    }

    @Test
    void rejectInvalidProgressLocationBeforeServiceCall() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(put("/api/v2/spaces/2/contents/3/reading-progress").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lastReadLocationId\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(progress);
    }

    @Test
    void findLastReading() throws Exception {
        givenValidMember(1L);
        var result = reading();
        var card = new ContentCardResponse(3L, ContentKind.BOOK,
                new ContentCardResponse.BookData("책", List.of(), null, 10, ContentCardResponse.Status.ACTIVE));
        given(activities.findLastClub(new MemberId(1L))).willReturn(Optional.of(result));
        given(spaceAdapters.get(SpaceKind.CLUB)).willReturn(spaceAdapter);
        given(spaceAdapter.mapReading(result.space())).willReturn(new ReadingResponses.ClubSpace("모임"));
        given(contentAdapters.card(result.content())).willReturn(card);

        mockMvc.perform(get("/api/v2/members/me/last-reading").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.space.spaceId").value(2))
                .andExpect(jsonPath("$.space.kind").value("CLUB"))
                .andExpect(jsonPath("$.space.data.name").value("모임"))
                .andExpect(jsonPath("$.content.contentId").value(3))
                .andExpect(jsonPath("$.content.kind").value("BOOK"))
                .andExpect(jsonPath("$.content.data.title").value("책"))
                .andExpect(jsonPath("$.content.data.authors").isArray())
                .andExpect(jsonPath("$.content.data.authors.length()").value(0))
                .andExpect(jsonPath("$.content.data.coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.content.data.passageCount").value(10))
                .andExpect(jsonPath("$.content.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.lastReadPassageSequence").value(2))
                .andExpect(jsonPath("$.progressRate").value(20))
                .andExpect(jsonPath("$.lastReadAt").value("2026-10-03T12:00:00"));
        verify(activities).findLastClub(new MemberId(1L));
        verify(spaceAdapters).get(SpaceKind.CLUB);
        verify(spaceAdapter).mapReading(result.space());
        verify(contentAdapters).card(result.content());
    }

    @Test
    void findRecentPublicRoomReading() throws Exception {
        givenValidMember(1L);
        var result = publicRoomReading();
        var card = new ContentCardResponse(3L, ContentKind.BOOK,
                new ContentCardResponse.BookData("책", List.of("작가"), "표지", 10,
                        ContentCardResponse.Status.DELETED));
        given(activities.findRecent(new MemberId(1L))).willReturn(Optional.of(result));
        given(spaceAdapters.get(SpaceKind.PUBLIC_ROOM)).willReturn(spaceAdapter);
        given(spaceAdapter.mapReading(result.space())).willReturn(new ReadingResponses.PublicRoomSpace());
        given(contentAdapters.card(result.content())).willReturn(card);
        mockMvc.perform(get("/api/v2/members/me/recent-reading").header("X-Member-Id", "1"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.space.spaceId").value(8))
                .andExpect(jsonPath("$.space.kind").value("PUBLIC_ROOM"))
                .andExpect(jsonPath("$.space.data").isMap())
                .andExpect(jsonPath("$.space.data.*").isEmpty())
                .andExpect(jsonPath("$.content.contentId").value(3))
                .andExpect(jsonPath("$.content.kind").value("BOOK"))
                .andExpect(jsonPath("$.content.data.title").value("책"))
                .andExpect(jsonPath("$.content.data.authors[0]").value("작가"))
                .andExpect(jsonPath("$.content.data.coverImageUrl").value("표지"))
                .andExpect(jsonPath("$.content.data.passageCount").value(10))
                .andExpect(jsonPath("$.content.data.status").value("DELETED"))
                .andExpect(jsonPath("$.lastReadPassageSequence").value(4))
                .andExpect(jsonPath("$.progressRate").value(40))
                .andExpect(jsonPath("$.lastReadAt").value("2026-10-03T12:00:00"));
        verify(activities).findRecent(new MemberId(1L));
        verify(spaceAdapters).get(SpaceKind.PUBLIC_ROOM);
        verify(spaceAdapter).mapReading(result.space());
        verify(contentAdapters).card(result.content());
    }

    @Test
    void findLastAndRecentReadingEmpty() throws Exception {
        givenValidMember(1L);
        given(activities.findLastClub(new MemberId(1L))).willReturn(Optional.empty());
        given(activities.findRecent(new MemberId(1L))).willReturn(Optional.empty());
        mockMvc.perform(get("/api/v2/members/me/last-reading").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mockMvc.perform(get("/api/v2/members/me/recent-reading").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(activities).findLastClub(new MemberId(1L));
        verify(activities).findRecent(new MemberId(1L));
    }

    @Test
    void propagateServiceException() throws Exception {
        givenValidMember(1L);
        var failure = new IllegalArgumentException("실패");
        given(activities.findRecent(new MemberId(1L))).willThrow(failure);
        var result = mockMvc.perform(get("/api/v2/members/me/recent-reading")
                .header("X-Member-Id", "1")).andReturn();
        assertSame(failure, result.getResolvedException(), "서비스 예외를 그대로 전파해야 한다");
        verify(activities).findRecent(new MemberId(1L));
    }

    private static ReadingActivityResult reading() {
        var space = new ReadingActivityResult.ReadingSpace(new SpaceId(2L), SpaceKind.CLUB,
                new ReadingActivityResult.ReadingSpace.Club("모임"));
        var content = new ContentCardResult(new ContentId(3L), ContentKind.BOOK, "책", List.of(), null, 10, true);
        return new ReadingActivityResult(space, content, 2, 20, LocalDateTime.of(2026, 10, 3, 12, 0));
    }

    private static ReadingActivityResult publicRoomReading() {
        var space = new ReadingActivityResult.ReadingSpace(new SpaceId(8L), SpaceKind.PUBLIC_ROOM,
                new ReadingActivityResult.ReadingSpace.PublicRoom());
        var content = new ContentCardResult(new ContentId(3L), ContentKind.BOOK, "책", List.of("작가"),
                "표지", 10, false);
        return new ReadingActivityResult(space, content, 4, 40, LocalDateTime.of(2026, 10, 3, 12, 0));
    }
}
