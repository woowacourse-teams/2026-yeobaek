package yeobaek.backend.comment.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

@WebMvcTest(CommentController.class)
class CommentControllerTest extends ControllerTest {

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("문단의 댓글 목록을 조회한다")
    void findComments() throws Exception {
        givenValidMember(1L);
        given(commentService.findComments(anyLong(), anyLong(), anyLong())).willReturn(new CommentsResponse(List.of(
                new CommentResponse(7L, 2L, "지수", "이 문장에서 멈칫했어요.",
                        LocalDateTime.of(2026, 8, 5, 14, 30), null, false))));

        mockMvc.perform(get("/api/clubs/{clubId}/passages/{passageId}/comments", 1L, 1042L).header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].commentId").value(7))
                .andExpect(jsonPath("$.comments[0].mine").value(false));
    }

    @Test
    @DisplayName("댓글을 작성하면 201과 mine=true를 응답한다")
    void createComment() throws Exception {
        givenValidMember(1L);
        given(commentService.create(anyLong(), anyLong(), anyLong(), anyString())).willReturn(
                new CommentResponse(7L, 1L, "민서", "이 문장에서 멈칫했어요.",
                        LocalDateTime.of(2026, 8, 5, 14, 30), null, true));

        mockMvc.perform(post("/api/clubs/{clubId}/passages/{passageId}/comments", 1L, 1042L)
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"이 문장에서 멈칫했어요.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true));
    }

    @Test
    @DisplayName("댓글을 수정하면 수정일이 포함된 댓글을 응답한다")
    void updateComment() throws Exception {
        givenValidMember(1L);
        given(commentService.update(anyLong(), anyLong(), anyString())).willReturn(
                new CommentResponse(7L, 1L, "민서", "수정된 내용",
                        LocalDateTime.of(2026, 8, 5, 14, 30), LocalDateTime.of(2026, 8, 5, 15, 0), true));

        mockMvc.perform(put("/api/comments/{commentId}", 7L)
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"수정된 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("댓글을 삭제하면 204를 응답한다")
    void deleteComment() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(delete("/api/comments/7").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("남의 댓글 수정은 403을 응답한다")
    void rejectUpdatingOthersComment() throws Exception {
        givenValidMember(1L);
        given(commentService.update(anyLong(), anyLong(), anyString()))
                .willThrow(new ForbiddenException(ErrorCode.NOT_COMMENT_OWNER, "본인의 댓글만 수정할 수 있습니다."));

        mockMvc.perform(put("/api/comments/7")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"탈취 시도\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인의 댓글만 수정할 수 있습니다."));
    }

    @Test
    @DisplayName("모임 미소속 회원의 댓글 작성은 403을 응답한다")
    void rejectOutsider() throws Exception {
        givenValidMember(1L);
        given(commentService.create(anyLong(), anyLong(), anyLong(), anyString()))
                .willThrow(new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));

        mockMvc.perform(post("/api/clubs/1/passages/1042/comments")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"댓글\"}"))
                .andExpect(status().isForbidden());
    }
}
