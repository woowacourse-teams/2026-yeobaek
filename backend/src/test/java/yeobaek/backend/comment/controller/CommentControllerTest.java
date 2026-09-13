package yeobaek.backend.comment.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentedSentenceResponse;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.dto.NewCommentCountResponse;
import yeobaek.backend.comment.domain.ContentVisibility;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@WebMvcTest(CommentController.class)
class CommentControllerTest extends ControllerTest {

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private AnalyticsTracker analyticsTracker;

    @Test
    @DisplayName("댓글 목록 요청을 서비스에 전달하고 nullable 필드를 포함한 전체 계약을 반환한다")
    void findComments() throws Exception {
        givenValidMember(1L);
        var createdAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        var updatedAt = LocalDateTime.of(2026, 8, 5, 15, 0);
        var response = new CommentsResponse(List.of(
                new CommentResponse(7L, 2L, "지수", "첫 댓글", createdAt, null, false),
                new CommentResponse(8L, 1L, "민서", "수정된 댓글", createdAt, updatedAt, true)));
        given(commentService.findComments(1L, 10L, 1042L)).willReturn(response);

        mockMvc.perform(get("/api/clubs/{clubId}/sentences/{sentenceId}/comments", 10L, 1042L)
                        .header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(7))
                .andExpect(jsonPath("$.comments[0].memberId").value(2))
                .andExpect(jsonPath("$.comments[0].nickname").value("지수"))
                .andExpect(jsonPath("$.comments[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$.comments[0].createdAt").value("2026-08-05T14:30:00"))
                .andExpect(jsonPath("$.comments[0].updatedAt").value((Object) null))
                .andExpect(jsonPath("$.comments[0].mine").value(false))
                .andExpect(jsonPath("$.comments[1].commentId").value(8))
                .andExpect(jsonPath("$.comments[1].memberId").value(1))
                .andExpect(jsonPath("$.comments[1].nickname").value("민서"))
                .andExpect(jsonPath("$.comments[1].content").value("수정된 댓글"))
                .andExpect(jsonPath("$.comments[1].createdAt").value("2026-08-05T14:30:00"))
                .andExpect(jsonPath("$.comments[1].updatedAt").value("2026-08-05T15:00:00"))
                .andExpect(jsonPath("$.comments[1].mine").value(true));

        verify(commentService, times(1)).findComments(1L, 10L, 1042L);
        verify(analyticsTracker, times(1))
                .track(1L, AnalyticsEvent.commentsViewed(10L, 1042L, 2));
    }

    @Test
    @DisplayName("댓글 상세 조회 요청을 서비스에 전달하고 보이는 댓글을 분석 이벤트로 기록한다")
    void findCommentDetails() throws Exception {
        givenValidMember(1L);
        var createdAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        var response = new CommentsResponse(List.of(
                new CommentResponse(7L, 2L, "지수", "첫 댓글", createdAt, null, false)));
        given(commentService.findComments(1L, 10L, 1042L)).willReturn(response);

        mockMvc.perform(post("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views", 10L, 1042L)
                        .header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(7))
                .andExpect(jsonPath("$.comments[0].memberId").value(2))
                .andExpect(jsonPath("$.comments[0].nickname").value("지수"))
                .andExpect(jsonPath("$.comments[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$.comments[0].createdAt").value("2026-08-05T14:30:00"))
                .andExpect(jsonPath("$.comments[0].updatedAt").value((Object) null))
                .andExpect(jsonPath("$.comments[0].mine").value(false));

        verify(commentService, times(1)).findComments(1L, 10L, 1042L);
        verify(analyticsTracker, times(1))
                .track(1L, AnalyticsEvent.commentsViewed(10L, 1042L, 1));
    }

    @Test
    @DisplayName("현재 문단까지의 새 댓글 개수를 반환한다")
    void countNewComments() throws Exception {
        givenValidMember(3L);
        given(commentService.countNewComments(3L, 10L, 1042L)).willReturn(new NewCommentCountResponse(5));

        mockMvc.perform(get("/api/clubs/{clubId}/comments/new-count", 10L)
                        .header("X-Member-Id", "3")
                        .queryParam("currentPassageId", "1042"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.newCommentCount").value(5));

        verify(commentService, times(1)).countNewComments(3L, 10L, 1042L);
    }

    @Test
    @DisplayName("댓글 문장 전체 목록의 모든 계약 필드를 반환한다")
    void findCommentedSentences() throws Exception {
        givenValidMember(4L);
        var latestCreatedAt = LocalDateTime.of(2026, 8, 7, 9, 10);
        var response = new CommentedSentencesResponse(List.of(new CommentedSentenceResponse(
                5012L, "가려질 문장", 1043L, 43, 2, true,
                3, 2, ContentVisibility.REVEAL_REQUIRED, latestCreatedAt)));
        given(commentService.findCommentedSentences(4L, 10L, 1042L)).willReturn(response);

        mockMvc.perform(get("/api/clubs/{clubId}/commented-sentences", 10L)
                        .header("X-Member-Id", "4")
                        .queryParam("currentPassageId", "1042"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.commentedSentences").isArray())
                .andExpect(jsonPath("$.commentedSentences.length()").value(1))
                .andExpect(jsonPath("$.commentedSentences[0].sentenceId").value(5012))
                .andExpect(jsonPath("$.commentedSentences[0].content").value("가려질 문장"))
                .andExpect(jsonPath("$.commentedSentences[0].passageId").value(1043))
                .andExpect(jsonPath("$.commentedSentences[0].passageSequence").value(43))
                .andExpect(jsonPath("$.commentedSentences[0].sentenceSequence").value(2))
                .andExpect(jsonPath("$.commentedSentences[0].future").value(true))
                .andExpect(jsonPath("$.commentedSentences[0].commentCount").value(3))
                .andExpect(jsonPath("$.commentedSentences[0].unreadCommentCount").value(2))
                .andExpect(jsonPath("$.commentedSentences[0].contentVisibility").value("REVEAL_REQUIRED"))
                .andExpect(jsonPath("$.commentedSentences[0].latestCommentCreatedAt")
                        .value("2026-08-07T09:10:00"));

        verify(commentService, times(1)).findCommentedSentences(4L, 10L, 1042L);
    }

    @Test
    @DisplayName("댓글 문장 목록이 비어 있으면 빈 배열을 반환한다")
    void findEmptyCommentedSentences() throws Exception {
        givenValidMember(5L);
        given(commentService.findCommentedSentences(5L, 10L, 1042L))
                .willReturn(new CommentedSentencesResponse(List.of()));

        mockMvc.perform(get("/api/clubs/{clubId}/commented-sentences", 10L)
                        .header("X-Member-Id", "5")
                        .queryParam("currentPassageId", "1042"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.commentedSentences").isArray())
                .andExpect(jsonPath("$.commentedSentences").isEmpty());

        verify(commentService, times(1)).findCommentedSentences(5L, 10L, 1042L);
    }

    @Test
    @DisplayName("필수 currentPassageId가 없으면 댓글 발견 서비스를 호출하지 않는다")
    void rejectMissingCurrentPassageId() throws Exception {
        givenValidMember(6L);

        mockMvc.perform(get("/api/clubs/{clubId}/comments/new-count", 10L)
                        .header("X-Member-Id", "6"))
                .andExpect(result -> assertInstanceOf(
                        MissingServletRequestParameterException.class,
                        result.getResolvedException()));

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("댓글 작성 요청을 서비스에 전달하고 전체 응답 계약을 반환한다")
    void createComment() throws Exception {
        givenValidMember(2L);
        var createdAt = LocalDateTime.of(2026, 8, 6, 9, 15);
        var response = new CommentResponse(9L, 2L, "민서", "새 댓글", createdAt, null, true);
        given(commentService.create(2L, 10L, 1042L, "새 댓글")).willReturn(response);

        mockMvc.perform(post("/api/clubs/{clubId}/sentences/{sentenceId}/comments", 10L, 1042L)
                        .header("X-Member-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"새 댓글"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.commentId").value(9))
                .andExpect(jsonPath("$.memberId").value(2))
                .andExpect(jsonPath("$.nickname").value("민서"))
                .andExpect(jsonPath("$.content").value("새 댓글"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-06T09:15:00"))
                .andExpect(jsonPath("$.updatedAt").value((Object) null))
                .andExpect(jsonPath("$.mine").value(true));

        verify(commentService, times(1)).create(2L, 10L, 1042L, "새 댓글");
        verify(analyticsTracker, times(1))
                .track(2L, AnalyticsEvent.commentCreated(10L, 1042L, 9L));
    }

    @Test
    @DisplayName("댓글이 없는 목록 조회는 분석 이벤트를 기록하지 않는다")
    void doNotTrackEmptyComments() throws Exception {
        givenValidMember(8L);
        given(commentService.findComments(8L, 10L, 1042L))
                .willReturn(new CommentsResponse(List.of()));

        mockMvc.perform(get("/api/clubs/{clubId}/sentences/{sentenceId}/comments", 10L, 1042L)
                        .header("X-Member-Id", "8"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());

        verify(commentService, times(1)).findComments(8L, 10L, 1042L);
        verifyNoInteractions(analyticsTracker);
    }

    @Test
    @DisplayName("댓글 수정 요청을 서비스에 전달하고 전체 응답 계약을 반환한다")
    void updateComment() throws Exception {
        givenValidMember(3L);
        var createdAt = LocalDateTime.of(2026, 8, 6, 9, 15);
        var updatedAt = LocalDateTime.of(2026, 8, 6, 10, 30);
        var response = new CommentResponse(9L, 3L, "민서", "수정된 내용", createdAt, updatedAt, true);
        given(commentService.update(3L, 9L, "수정된 내용")).willReturn(response);

        mockMvc.perform(put("/api/comments/{commentId}", 9L)
                        .header("X-Member-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정된 내용"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.commentId").value(9))
                .andExpect(jsonPath("$.memberId").value(3))
                .andExpect(jsonPath("$.nickname").value("민서"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-06T09:15:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-08-06T10:30:00"))
                .andExpect(jsonPath("$.mine").value(true));

        verify(commentService, times(1)).update(3L, 9L, "수정된 내용");
    }

    @Test
    @DisplayName("댓글 삭제 요청을 서비스에 전달하고 빈 204 응답을 반환한다")
    void deleteComment() throws Exception {
        givenValidMember(4L);

        mockMvc.perform(delete("/api/comments/{commentId}", 9L)
                        .header("X-Member-Id", "4"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(commentService, times(1)).delete(4L, 9L);
    }

    @Test
    @DisplayName("댓글 신고 요청을 서비스에 전달하고 빈 204 응답을 반환한다")
    void reportComment() throws Exception {
        givenValidMember(4L);

        mockMvc.perform(post("/api/comments/{commentId}/reports", 9L)
                        .header("X-Member-Id", "4"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(commentService, times(1)).report(4L, 9L);
    }

    @Test
    @DisplayName("댓글 작성 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingCreateBody() throws Exception {
        givenValidMember(5L);

        mockMvc.perform(post("/api/clubs/{clubId}/sentences/{sentenceId}/comments", 10L, 1042L)
                        .header("X-Member-Id", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(commentService);
        verifyNoInteractions(analyticsTracker);
    }

    @Test
    @DisplayName("댓글 수정 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingUpdateBody() throws Exception {
        givenValidMember(6L);

        mockMvc.perform(put("/api/comments/{commentId}", 9L)
                        .header("X-Member-Id", "6")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(7L);
        var serviceException = new NotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        given(commentService.findComments(7L, 999L, 1042L)).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/clubs/{clubId}/sentences/{sentenceId}/comments", 999L, 1042L)
                        .header("X-Member-Id", "7"))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(commentService, times(1)).findComments(7L, 999L, 1042L);
        verifyNoInteractions(analyticsTracker);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/api/clubs/10/comments/new-count", "/api/clubs/10/commented-sentences"
    })
    @DisplayName("발견 API의 currentPassageId 숫자 형식 오류는 서비스 호출 전에 거부한다")
    void rejectMalformedCurrentPassage(String path) throws Exception {
        givenValidMember(1L);
        mockMvc.perform(get(path).header("X-Member-Id", "1").param("currentPassageId", "abc"))
                .andExpect(result -> assertInstanceOf(
                        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("문장 전체 목록에도 currentPassageId가 필수다")
    void rejectMissingListCurrentPassage() throws Exception {
        givenValidMember(1L);
        mockMvc.perform(get("/api/clubs/10/commented-sentences").header("X-Member-Id", "1"))
                .andExpect(result -> assertInstanceOf(MissingServletRequestParameterException.class,
                        result.getResolvedException()));
        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("새 상세 POST의 빈 목록은 이벤트 없이 빈 배열을 반환한다")
    void emptyPostDetails() throws Exception {
        givenValidMember(1L);
        given(commentService.findComments(1L, 10L, 1042L)).willReturn(new CommentsResponse(List.of()));
        mockMvc.perform(post("/api/clubs/10/sentences/1042/comment-detail-views").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());
        verify(commentService, times(1)).findComments(1L, 10L, 1042L);
        verifyNoInteractions(analyticsTracker);
    }
}
