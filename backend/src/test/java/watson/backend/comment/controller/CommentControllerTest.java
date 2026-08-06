package watson.backend.comment.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.comment.dto.CommentResponse;
import watson.backend.comment.dto.CommentsResponse;
import watson.backend.comment.service.CommentService;
import watson.backend.support.ControllerTest;
import watson.backend.support.ErrorCode;
import watson.backend.support.ForbiddenException;

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

        mockMvc.perform(get("/api/clubs/1/passages/1042/comments").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].commentId").value(7))
                .andExpect(jsonPath("$.comments[0].mine").value(false))
                .andDo(document("comment-list", resource(ResourceSnippetParameters.builder()
                        .tag("댓글")
                        .summary("문단의 댓글 목록 조회")
                        .description("작성일 오름차순. 이 모임에서 작성된 댓글만 보인다.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("comments[].commentId").description("댓글 ID"),
                                PayloadDocumentation.fieldWithPath("comments[].memberId").description("작성자 회원 ID"),
                                PayloadDocumentation.fieldWithPath("comments[].nickname").description("작성자 닉네임"),
                                PayloadDocumentation.fieldWithPath("comments[].content").description("내용"),
                                PayloadDocumentation.fieldWithPath("comments[].createdAt").description("작성일"),
                                PayloadDocumentation.fieldWithPath("comments[].updatedAt").description("수정일 (수정된 적 없으면 null)").optional(),
                                PayloadDocumentation.fieldWithPath("comments[].mine").description("요청자 본인 작성 여부"))
                        .build())));
    }

    @Test
    @DisplayName("댓글을 작성하면 201과 mine=true를 응답한다")
    void createComment() throws Exception {
        givenValidMember(1L);
        given(commentService.create(anyLong(), anyLong(), anyLong(), anyString())).willReturn(
                new CommentResponse(7L, 1L, "민서", "이 문장에서 멈칫했어요.",
                        LocalDateTime.of(2026, 8, 5, 14, 30), null, true));

        mockMvc.perform(post("/api/clubs/1/passages/1042/comments")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"이 문장에서 멈칫했어요.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andDo(document("comment-create", resource(ResourceSnippetParameters.builder()
                        .tag("댓글")
                        .summary("댓글 작성")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .requestFields(PayloadDocumentation.fieldWithPath("content").description("내용 (1~1000자)"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("commentId").description("댓글 ID"),
                                PayloadDocumentation.fieldWithPath("memberId").description("작성자 회원 ID"),
                                PayloadDocumentation.fieldWithPath("nickname").description("작성자 닉네임"),
                                PayloadDocumentation.fieldWithPath("content").description("내용"),
                                PayloadDocumentation.fieldWithPath("createdAt").description("작성일"),
                                PayloadDocumentation.fieldWithPath("updatedAt").description("수정일 (작성 직후 null)").optional(),
                                PayloadDocumentation.fieldWithPath("mine").description("요청자 본인 작성 여부 (항상 true)"))
                        .build())));
    }

    @Test
    @DisplayName("댓글을 수정하면 수정일이 포함된 댓글을 응답한다")
    void updateComment() throws Exception {
        givenValidMember(1L);
        given(commentService.update(anyLong(), anyLong(), anyString())).willReturn(
                new CommentResponse(7L, 1L, "민서", "수정된 내용",
                        LocalDateTime.of(2026, 8, 5, 14, 30), LocalDateTime.of(2026, 8, 5, 15, 0), true));

        mockMvc.perform(put("/api/comments/7")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"수정된 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andDo(document("comment-update", resource(ResourceSnippetParameters.builder()
                        .tag("댓글")
                        .summary("댓글 수정")
                        .description("본인 댓글이 아니면 403.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .requestFields(PayloadDocumentation.fieldWithPath("content").description("수정할 내용 (1~1000자)"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("commentId").description("댓글 ID"),
                                PayloadDocumentation.fieldWithPath("memberId").description("작성자 회원 ID"),
                                PayloadDocumentation.fieldWithPath("nickname").description("작성자 닉네임"),
                                PayloadDocumentation.fieldWithPath("content").description("내용"),
                                PayloadDocumentation.fieldWithPath("createdAt").description("작성일"),
                                PayloadDocumentation.fieldWithPath("updatedAt").description("수정일"),
                                PayloadDocumentation.fieldWithPath("mine").description("요청자 본인 작성 여부"))
                        .build())));
    }

    @Test
    @DisplayName("댓글을 삭제하면 204를 응답한다")
    void deleteComment() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(delete("/api/comments/7").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent())
                .andDo(document("comment-delete", resource(ResourceSnippetParameters.builder()
                        .tag("댓글")
                        .summary("댓글 삭제")
                        .description("하드 삭제(PRD 3.5). 본인 댓글이 아니면 403.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .build())));
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
