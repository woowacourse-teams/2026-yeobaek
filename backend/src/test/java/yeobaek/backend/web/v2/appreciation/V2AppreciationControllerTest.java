package yeobaek.backend.web.v2.appreciation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(V2AppreciationController.class)
@Import(CommentAppreciationJsonConfiguration.class)
class V2AppreciationControllerTest extends ControllerTest {

    @MockitoBean private AppreciationWebAdapterRegistry adapters;
    @MockitoBean private AppreciationWebAdapter adapter;
    @MockitoBean private CommentQueryWorkflow comments;
    @MockitoBean private CommentModificationWorkflow modifications;
    @MockitoBean private CommentReportWorkflow reports;

    @Test
    void findViewCreateAndUpdate() throws Exception {
        givenValidMember(1L);
        var response = appreciation();
        given(adapters.get(AppreciationKind.COMMENT)).willReturn(adapter);
        given(adapter.find(new MemberId(1L), new SpaceId(2L), new ContentId(3L), new ContentLocationId(4L)))
                .willReturn(List.of(response));
        given(adapter.create(org.mockito.ArgumentMatchers.eq(new MemberId(1L)),
                org.mockito.ArgumentMatchers.eq(new SpaceId(2L)), org.mockito.ArgumentMatchers.eq(new ContentId(3L)),
                org.mockito.ArgumentMatchers.eq(new ContentLocationId(4L)), org.mockito.ArgumentMatchers.any()))
                .willReturn(response);
        given(adapter.update(org.mockito.ArgumentMatchers.eq(new MemberId(1L)),
                org.mockito.ArgumentMatchers.eq(new AppreciationId(5L)), org.mockito.ArgumentMatchers.any()))
                .willReturn(response);

        var found = mockMvc.perform(get("/api/v2/spaces/2/contents/3/locations/4/appreciations")
                        .header("X-Member-Id", "1").param("kind", "COMMENT"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.appreciations").isArray())
                .andExpect(jsonPath("$.appreciations.length()").value(1));
        expectAppreciation(found, "$.appreciations[0]");
        var viewed = mockMvc.perform(post("/api/v2/spaces/2/contents/3/locations/4/appreciation-views")
                        .header("X-Member-Id", "1"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.appreciations.length()").value(1));
        expectAppreciation(viewed, "$.appreciations[0]");
        var created = mockMvc.perform(post("/api/v2/spaces/2/contents/3/locations/4/appreciations")
                        .header("X-Member-Id", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"content\":\"감상\"},\"kind\":\"COMMENT\"}"))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        expectAppreciation(created, "$");
        var updated = mockMvc.perform(put("/api/v2/appreciations/5").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"COMMENT\",\"data\":{\"content\":\"수정\"}}"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        expectAppreciation(updated, "$");

        verifyCreateRequest();
        verifyUpdateRequest();
        verify(adapter, times(2)).find(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L));
    }

    @Test
    void unreadAndDiscovery() throws Exception {
        givenValidMember(1L);
        given(comments.countNewComments(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L))).willReturn(7L);
        given(comments.findDiscovery(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L))).willReturn(List.of(new CommentQueryWorkflow.DiscoveryResult(
                        new ContentLocationId(5L), "문장", new ContentLocationId(4L), 2, 1, false,
                        3, 1, true, LocalDateTime.of(2026, 10, 3, 12, 0))));

        mockMvc.perform(get("/api/v2/spaces/2/contents/3/appreciations/unread-count")
                        .header("X-Member-Id", "1").param("currentLocationId", "4"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.newCommentCount").value(7));
        mockMvc.perform(get("/api/v2/spaces/2/contents/3/appreciation-discovery")
                        .header("X-Member-Id", "1").param("currentLocationId", "4"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.commentedLocations").isArray())
                .andExpect(jsonPath("$.commentedLocations.length()").value(1))
                .andExpect(jsonPath("$.commentedLocations[0].locationId").value(5))
                .andExpect(jsonPath("$.commentedLocations[0].content").value("문장"))
                .andExpect(jsonPath("$.commentedLocations[0].passageLocationId").value(4))
                .andExpect(jsonPath("$.commentedLocations[0].passageSequence").value(2))
                .andExpect(jsonPath("$.commentedLocations[0].sentenceSequence").value(1))
                .andExpect(jsonPath("$.commentedLocations[0].future").value(false))
                .andExpect(jsonPath("$.commentedLocations[0].commentCount").value(3))
                .andExpect(jsonPath("$.commentedLocations[0].unreadCommentCount").value(1))
                .andExpect(jsonPath("$.commentedLocations[0].contentVisibility").value("REVEAL_REQUIRED"))
                .andExpect(jsonPath("$.commentedLocations[0].latestCommentCreatedAt")
                        .value("2026-10-03T12:00:00"));
        verify(comments).countNewComments(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L));
        verify(comments).findDiscovery(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L));
    }

    @Test
    void deleteAndReport() throws Exception {
        givenValidMember(1L);
        mockMvc.perform(delete("/api/v2/appreciations/5").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mockMvc.perform(post("/api/v2/appreciations/5/reports").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(modifications).delete(new MemberId(1L), new AppreciationId(5L));
        verify(reports).report(new MemberId(1L), new AppreciationId(5L));
    }

    @Test
    void findEmptyAppreciations() throws Exception {
        givenValidMember(1L);
        given(adapters.get(AppreciationKind.COMMENT)).willReturn(adapter);
        given(adapter.find(new MemberId(1L), new SpaceId(2L), new ContentId(3L), new ContentLocationId(4L)))
                .willReturn(List.of());
        mockMvc.perform(get("/api/v2/spaces/2/contents/3/locations/4/appreciations")
                        .header("X-Member-Id", "1").param("kind", "COMMENT"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.appreciations").isArray())
                .andExpect(jsonPath("$.appreciations.length()").value(0));
        verify(adapter).find(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L));
    }

    @Test
    void rejectInvalidLocationIdBeforeAdapterCall() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(get("/api/v2/spaces/2/contents/3/locations/0/appreciations")
                        .header("X-Member-Id", "1").param("kind", "COMMENT"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adapters, adapter);
    }

    @Test
    void rejectMissingKindAndData() throws Exception {
        givenValidMember(1L);
        mockMvc.perform(post("/api/v2/spaces/2/contents/3/locations/4/appreciations")
                        .header("X-Member-Id", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"content\":\"감상\"}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adapter);
    }

    @Test
    void rejectUnknownKindNullDataAndMissingComment() throws Exception {
        givenValidMember(1L);
        for (String body : List.of("{\"kind\":\"UNKNOWN\",\"data\":{}}",
                "{\"kind\":\"COMMENT\"}",
                "{\"kind\":\"COMMENT\",\"data\":null}",
                "{\"kind\":\"COMMENT\",\"data\":{}}",
                "{\"kind\":\"COMMENT\",\"data\":{\"name\":\"모임\",\"contentId\":2}}")) {
            mockMvc.perform(post("/api/v2/spaces/2/contents/3/locations/4/appreciations")
                            .header("X-Member-Id", "1").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(adapter);
    }

    @Test
    void propagateServiceException() throws Exception {
        givenValidMember(1L);
        var failure = new IllegalArgumentException("실패");
        given(comments.countNewComments(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L))).willThrow(failure);
        var result = mockMvc.perform(get("/api/v2/spaces/2/contents/3/appreciations/unread-count")
                .header("X-Member-Id", "1").param("currentLocationId", "4")).andReturn();
        assertSame(failure, result.getResolvedException(), "서비스 예외를 그대로 전파해야 한다");
        verify(comments).countNewComments(new MemberId(1L), new SpaceId(2L), new ContentId(3L),
                new ContentLocationId(4L));
    }

    private static AppreciationResponses.Appreciation appreciation() {
        return new AppreciationResponses.Appreciation(5L, AppreciationKind.COMMENT, 1L, "회원",
                LocalDateTime.of(2026, 10, 3, 12, 0), null, true,
                new AppreciationResponses.CommentData("감상"));
    }

    private void verifyCreateRequest() {
        var captor = ArgumentCaptor.forClass(AppreciationRequests.Data.class);
        verify(adapter).create(org.mockito.ArgumentMatchers.eq(new MemberId(1L)),
                org.mockito.ArgumentMatchers.eq(new SpaceId(2L)), org.mockito.ArgumentMatchers.eq(new ContentId(3L)),
                org.mockito.ArgumentMatchers.eq(new ContentLocationId(4L)), captor.capture());
        var data = assertInstanceOf(AppreciationRequests.CommentData.class, captor.getValue(),
                "댓글 입력 타입이어야 한다");
        assertEquals("감상", data.content().value(), "댓글 내용을 보존해야 한다");
    }

    private void verifyUpdateRequest() {
        var captor = ArgumentCaptor.forClass(AppreciationRequests.Data.class);
        verify(adapter).update(org.mockito.ArgumentMatchers.eq(new MemberId(1L)),
                org.mockito.ArgumentMatchers.eq(new AppreciationId(5L)), captor.capture());
        var data = assertInstanceOf(AppreciationRequests.CommentData.class, captor.getValue(),
                "댓글 수정 타입이어야 한다");
        assertEquals("수정", data.content().value(), "수정 내용을 보존해야 한다");
    }

    private static void expectAppreciation(org.springframework.test.web.servlet.ResultActions performed,
                                            String path) throws Exception {
        performed.andExpect(jsonPath(path + ".appreciationId").value(5))
                .andExpect(jsonPath(path + ".kind").value("COMMENT"))
                .andExpect(jsonPath(path + ".memberId").value(1))
                .andExpect(jsonPath(path + ".nickname").value("회원"))
                .andExpect(jsonPath(path + ".createdAt").value("2026-10-03T12:00:00"))
                .andExpect(jsonPath(path + ".updatedAt").value((Object) null))
                .andExpect(jsonPath(path + ".mine").value(true))
                .andExpect(jsonPath(path + ".data.content").value("감상"));
    }
}
