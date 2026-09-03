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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
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
}
