package yeobaek.backend.web.v2.space;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import yeobaek.backend.application.club.ClubMembershipWorkflow;
import yeobaek.backend.application.publicroom.PublicRoomVisitWorkflow;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryService;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.web.v2.common.ContentCardResponse;

@WebMvcTest(V2SpaceController.class)
@Import(ClubSpaceJsonConfiguration.class)
class V2SpaceControllerTest extends ControllerTest {

    @MockitoBean private SpaceWebAdapterRegistry adapters;
    @MockitoBean private SpaceWebAdapter adapter;
    @MockitoBean private SpaceQueryService spaces;
    @MockitoBean private ClubMembershipWorkflow memberships;
    @MockitoBean private PublicRoomVisitWorkflow visits;

    @Test
    void createClub() throws Exception {
        givenValidMember(1L);
        var response = new SpaceResponses.Space(7L, SpaceKind.CLUB,
                new SpaceResponses.CreatedClub("모임", "ABC123", card()));
        given(adapters.get(SpaceKind.CLUB)).willReturn(adapter);
        given(adapter.create(org.mockito.ArgumentMatchers.eq(new MemberId(1L)),
                org.mockito.ArgumentMatchers.any())).willReturn(response);
        var performed = mockMvc.perform(post("/api/v2/spaces").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"CLUB\",\"data\":{\"name\":\"모임\",\"contentId\":2}}"))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaceId").value(7)).andExpect(jsonPath("$.kind").value("CLUB"))
                .andExpect(jsonPath("$.data.name").value("모임"))
                .andExpect(jsonPath("$.data.joinCode").value("ABC123"));
        expectCard(performed, "$.data.content");
        var captor = ArgumentCaptor.forClass(SpaceRequests.CreateData.class);
        verify(adapter).create(org.mockito.ArgumentMatchers.eq(new MemberId(1L)), captor.capture());
        var data = assertInstanceOf(SpaceRequests.CreateClubData.class, captor.getValue());
        assertEquals("모임", data.name().value(), "모임 이름을 역직렬화해야 한다");
        assertEquals(new ContentId(2L), data.contentId(), "컨텐츠 ID를 VO로 역직렬화해야 한다");
    }

    @Test
    void joinClubWithDataBeforeKind() throws Exception {
        givenValidMember(1L);
        var response = new SpaceResponses.Space(7L, SpaceKind.CLUB,
                new SpaceResponses.JoinedClub("모임", card()));
        given(adapters.get(SpaceKind.CLUB)).willReturn(adapter);
        given(adapter.join(org.mockito.ArgumentMatchers.eq(new MemberId(1L)),
                org.mockito.ArgumentMatchers.any())).willReturn(response);
        var performed = mockMvc.perform(post("/api/v2/spaces/join").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"joinCode\":\"ABC123\"},\"kind\":\"CLUB\"}"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaceId").value(7)).andExpect(jsonPath("$.kind").value("CLUB"))
                .andExpect(jsonPath("$.data.name").value("모임"));
        expectCard(performed, "$.data.content");
        var captor = ArgumentCaptor.forClass(SpaceRequests.JoinData.class);
        verify(adapter).join(org.mockito.ArgumentMatchers.eq(new MemberId(1L)), captor.capture());
        assertEquals("ABC123", assertInstanceOf(SpaceRequests.JoinClubData.class,
                captor.getValue(), "모임 가입 타입이어야 한다").joinCode().value(), "참여 코드를 역직렬화해야 한다");
    }

    @Test
    void leaveClub() throws Exception {
        givenValidMember(1L);
        mockMvc.perform(delete("/api/v2/spaces/7/members/me").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(memberships).leave(new MemberId(1L), new SpaceId(7L));
    }

    @Test
    void findMineClub() throws Exception {
        givenValidMember(1L);
        var result = result(7L, SpaceKind.CLUB);
        var response = new SpaceResponses.Space(7L, SpaceKind.CLUB,
                new SpaceResponses.ClubSummary("모임", 2, card(), progress()));
        given(adapters.get(SpaceKind.CLUB)).willReturn(adapter);
        given(spaces.findMine(new MemberId(1L), SpaceKind.CLUB)).willReturn(List.of(result));
        given(adapter.map(result)).willReturn(response);
        var performed = mockMvc.perform(get("/api/v2/members/me/spaces").header("X-Member-Id", "1")
                        .param("kind", "CLUB"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaces").isArray()).andExpect(jsonPath("$.spaces.length()").value(1))
                .andExpect(jsonPath("$.spaces[0].spaceId").value(7))
                .andExpect(jsonPath("$.spaces[0].kind").value("CLUB"))
                .andExpect(jsonPath("$.spaces[0].data.name").value("모임"))
                .andExpect(jsonPath("$.spaces[0].data.memberCount").value(2));
        expectCard(performed, "$.spaces[0].data.content");
        expectProgress(performed, "$.spaces[0].data.myProgress");
        verify(spaces).findMine(new MemberId(1L), SpaceKind.CLUB);
        verify(adapter).map(result);
    }

    @Test
    void findMinePublicRoomIncludesVisitedAt() throws Exception {
        givenValidMember(1L);
        var result = result(8L, SpaceKind.PUBLIC_ROOM);
        var response = new SpaceResponses.Space(8L, SpaceKind.PUBLIC_ROOM,
                new SpaceResponses.PublicRoomVisited(card(), null, time()));
        given(adapters.get(SpaceKind.PUBLIC_ROOM)).willReturn(adapter);
        given(spaces.findMine(new MemberId(1L), SpaceKind.PUBLIC_ROOM)).willReturn(List.of(result));
        given(adapter.map(result)).willReturn(response);
        var performed = mockMvc.perform(get("/api/v2/members/me/spaces").header("X-Member-Id", "1")
                        .param("kind", "PUBLIC_ROOM"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaces.length()").value(1))
                .andExpect(jsonPath("$.spaces[0].spaceId").value(8))
                .andExpect(jsonPath("$.spaces[0].kind").value("PUBLIC_ROOM"))
                .andExpect(jsonPath("$.spaces[0].data.myProgress").value((Object) null))
                .andExpect(jsonPath("$.spaces[0].data.lastVisitedAt").value("2026-10-03T12:00:00"));
        expectCard(performed, "$.spaces[0].data.content");
        verify(spaces).findMine(new MemberId(1L), SpaceKind.PUBLIC_ROOM);
        verify(adapter).map(result);
    }

    @Test
    void findClubDetail() throws Exception {
        givenValidMember(1L);
        var result = result(7L, SpaceKind.CLUB);
        var response = new SpaceResponses.Space(7L, SpaceKind.CLUB,
                new SpaceResponses.ClubDetail("모임", "ABC123", card(), progress(),
                        List.of(new SpaceResponses.Member(1L, "회원", true, false))));
        given(spaces.findDetail(new MemberId(1L), new SpaceId(7L))).willReturn(result);
        given(adapters.get(SpaceKind.CLUB)).willReturn(adapter);
        given(adapter.map(result)).willReturn(response);
        var performed = mockMvc.perform(get("/api/v2/spaces/7").header("X-Member-Id", "1"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaceId").value(7))
                .andExpect(jsonPath("$.kind").value("CLUB"))
                .andExpect(jsonPath("$.data.name").value("모임"))
                .andExpect(jsonPath("$.data.joinCode").value("ABC123"))
                .andExpect(jsonPath("$.data.members").isArray()).andExpect(jsonPath("$.data.members.length()").value(1))
                .andExpect(jsonPath("$.data.members[0].memberId").value(1))
                .andExpect(jsonPath("$.data.members[0].nickname").value("회원"))
                .andExpect(jsonPath("$.data.members[0].mine").value(true))
                .andExpect(jsonPath("$.data.members[0].blocked").value(false));
        expectCard(performed, "$.data.content");
        expectProgress(performed, "$.data.myProgress");
        verify(spaces).findDetail(new MemberId(1L), new SpaceId(7L));
        verify(adapters).get(SpaceKind.CLUB);
        verify(adapter).map(result);
    }

    @Test
    void findPublicRoomDetail() throws Exception {
        givenValidMember(1L);
        var result = result(8L, SpaceKind.PUBLIC_ROOM);
        var response = new SpaceResponses.Space(8L, SpaceKind.PUBLIC_ROOM,
                new SpaceResponses.PublicRoomDetail(card(), progress(), time()));
        given(spaces.findDetail(new MemberId(1L), new SpaceId(8L))).willReturn(result);
        given(adapters.get(SpaceKind.PUBLIC_ROOM)).willReturn(adapter);
        given(adapter.map(result)).willReturn(response);
        var performed = mockMvc.perform(get("/api/v2/spaces/8").header("X-Member-Id", "1"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaceId").value(8)).andExpect(jsonPath("$.kind").value("PUBLIC_ROOM"))
                .andExpect(jsonPath("$.data.lastVisitedAt").value("2026-10-03T12:00:00"));
        expectCard(performed, "$.data.content");
        expectProgress(performed, "$.data.myProgress");
        verify(spaces).findDetail(new MemberId(1L), new SpaceId(8L));
        verify(adapters).get(SpaceKind.PUBLIC_ROOM);
        verify(adapter).map(result);
    }

    @Test
    void findPublicRoomsWithoutSort() throws Exception {
        givenValidMember(1L);
        var result = result(8L, SpaceKind.PUBLIC_ROOM);
        var response = new SpaceResponses.Space(8L, SpaceKind.PUBLIC_ROOM,
                new SpaceResponses.PublicRoomSummary(card(), null));
        given(adapters.get(SpaceKind.PUBLIC_ROOM)).willReturn(adapter);
        given(adapter.findPublic(new MemberId(1L), null)).willReturn(List.of(result));
        given(adapter.map(result)).willReturn(response);
        var performed = mockMvc.perform(get("/api/v2/spaces").header("X-Member-Id", "1")
                        .param("kind", "PUBLIC_ROOM"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spaces.length()").value(1))
                .andExpect(jsonPath("$.spaces[0].spaceId").value(8))
                .andExpect(jsonPath("$.spaces[0].kind").value("PUBLIC_ROOM"))
                .andExpect(jsonPath("$.spaces[0].data.myProgress").value((Object) null))
                .andExpect(jsonPath("$.spaces[0].data.lastVisitedAt").doesNotExist());
        expectCard(performed, "$.spaces[0].data.content");
        verify(adapter).findPublic(new MemberId(1L), null);
        verify(adapter).map(result);
    }

    @Test
    void visitPublicRoom() throws Exception {
        givenValidMember(1L);
        mockMvc.perform(post("/api/v2/spaces/8/visits").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(visits).visit(new MemberId(1L), new SpaceId(8L));
    }

    @Test
    void rejectInvalidCreateAndJoinBodiesBeforeAdapterCall() throws Exception {
        givenValidMember(1L);
        List<String> createBodies = List.of("{\"data\":{\"name\":\"모임\",\"contentId\":2}}",
                "{\"kind\":\"CLUB\"}", "{\"kind\":\"CLUB\",\"data\":null}",
                "{\"kind\":\"CLUB\",\"data\":{\"name\":\"모임\"}}",
                "{\"kind\":\"CLUB\",\"data\":{\"name\":\"모임\",\"contentId\":0}}",
                "{\"kind\":\"CLUB\",\"data\":{\"joinCode\":\"ABC123\"}}",
                "{\"kind\":\"UNKNOWN\",\"data\":{}}");
        for (String body : createBodies) {
            mockMvc.perform(post("/api/v2/spaces").header("X-Member-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        List<String> joinBodies = List.of("{\"data\":{\"joinCode\":\"ABC123\"}}",
                "{\"kind\":\"CLUB\"}", "{\"kind\":\"CLUB\",\"data\":null}",
                "{\"kind\":\"CLUB\",\"data\":{}}",
                "{\"kind\":\"CLUB\",\"data\":{\"name\":\"모임\",\"contentId\":2}}",
                "{\"kind\":\"UNKNOWN\",\"data\":{}}");
        for (String body : joinBodies) {
            mockMvc.perform(post("/api/v2/spaces/join").header("X-Member-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(adapters, adapter);
    }

    @Test
    void propagateServiceException() throws Exception {
        givenValidMember(1L);
        var failure = new IllegalArgumentException("실패");
        given(spaces.findDetail(new MemberId(1L), new SpaceId(99L))).willThrow(failure);
        var result = mockMvc.perform(get("/api/v2/spaces/99").header("X-Member-Id", "1")).andReturn();
        assertSame(failure, result.getResolvedException(), "서비스 예외를 그대로 전파해야 한다");
        verify(spaces).findDetail(new MemberId(1L), new SpaceId(99L));
    }

    private static SpaceQueryResult result(long id, SpaceKind kind) {
        return new SpaceQueryResult(new SpaceId(id), kind, org.mockito.Mockito.mock(SpaceQueryResult.Data.class));
    }

    private static ContentCardResponse card() {
        return new ContentCardResponse(2L, ContentKind.BOOK,
                new ContentCardResponse.BookData("책", List.of("작가"), null, 10,
                        ContentCardResponse.Status.ACTIVE));
    }

    private static SpaceResponses.Progress progress() {
        return new SpaceResponses.Progress(5, 50, time());
    }

    private static LocalDateTime time() {
        return LocalDateTime.of(2026, 10, 3, 12, 0);
    }

    private static void expectCard(ResultActions performed, String path) throws Exception {
        performed.andExpect(jsonPath(path + ".contentId").value(2))
                .andExpect(jsonPath(path + ".kind").value("BOOK"))
                .andExpect(jsonPath(path + ".data.title").value("책"))
                .andExpect(jsonPath(path + ".data.authors").isArray())
                .andExpect(jsonPath(path + ".data.authors.length()").value(1))
                .andExpect(jsonPath(path + ".data.authors[0]").value("작가"))
                .andExpect(jsonPath(path + ".data.coverImageUrl").value((Object) null))
                .andExpect(jsonPath(path + ".data.passageCount").value(10))
                .andExpect(jsonPath(path + ".data.status").value("ACTIVE"));
    }

    private static void expectProgress(ResultActions performed, String path) throws Exception {
        performed.andExpect(jsonPath(path + ".lastReadPassageSequence").value(5))
                .andExpect(jsonPath(path + ".progressRate").value(50))
                .andExpect(jsonPath(path + ".lastReadAt").value("2026-10-03T12:00:00"));
    }
}
