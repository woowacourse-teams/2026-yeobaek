package yeobaek.backend.publicroom.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.dto.NewCommentCountResponse;
import yeobaek.backend.comment.service.CommentService;
import yeobaek.backend.publicroom.dto.PublicRoomDetailResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressResponse;
import yeobaek.backend.publicroom.dto.PublicRoomResponse;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.dto.PublicRoomsResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomsResponse;
import yeobaek.backend.publicroom.service.PublicRoomService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(PublicRoomController.class)
class PublicRoomControllerTest extends ControllerTest {

    @MockitoBean
    private PublicRoomService publicRoomService;

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("공개방 목록의 생략 정렬을 기본값으로 서비스에 전달한다")
    void findAll() throws Exception {
        givenValidMember(1L);
        var response = new PublicRoomsResponse(List.of(new PublicRoomResponse(2L, book(), null)));
        given(publicRoomService.findAll(1L, PublicRoomSort.MOST_VISITED)).willReturn(response);

        mockMvc.perform(get("/api/public-rooms").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicRooms[0].publicRoomId").value(2));

        verify(publicRoomService).findAll(1L, PublicRoomSort.MOST_VISITED);
    }

    @Test
    @DisplayName("방문 공개방 목록 요청을 전달한다")
    void findVisited() throws Exception {
        givenValidMember(1L);
        given(publicRoomService.findVisited(1L)).willReturn(new VisitedPublicRoomsResponse(List.of()));

        mockMvc.perform(get("/api/members/me/public-rooms").header("X-Member-Id", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publicRooms").isEmpty());

        verify(publicRoomService).findVisited(1L);
    }

    @Test
    @DisplayName("공개방 상세 응답은 방문 이력이 없으면 null을 포함한다")
    void findDetail() throws Exception {
        givenValidMember(1L);
        given(publicRoomService.findDetail(1L, 2L))
                .willReturn(new PublicRoomDetailResponse(2L, book(), null, null));

        mockMvc.perform(get("/api/public-rooms/2").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastVisitedAt").value((Object) null));

        verify(publicRoomService).findDetail(1L, 2L);
    }

    @Test
    @DisplayName("공개방 방문은 빈 204를 반환한다")
    void visit() throws Exception {
        givenValidMember(1L);

        mockMvc.perform(post("/api/public-rooms/2/visits").header("X-Member-Id", "1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(publicRoomService).visit(1L, 2L);
    }

    @Test
    @DisplayName("본문 범위 파라미터를 서비스에 전달한다")
    void findPassages() throws Exception {
        givenValidMember(1L);
        given(publicRoomService.findPassages(1L, 2L, 3, 4)).willReturn(new PassagesResponse(List.of()));

        mockMvc.perform(get("/api/public-rooms/2/passages").header("X-Member-Id", "1")
                        .queryParam("from", "3").queryParam("to", "4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passages").isEmpty());

        verify(publicRoomService).findPassages(1L, 2L, 3, 4);
    }

    @Test
    @DisplayName("공개방 진도 요청 본문을 서비스에 전달한다")
    void updateProgress() throws Exception {
        givenValidMember(1L);
        var response = new PublicRoomProgressResponse(4, 40, LocalDateTime.of(2026, 9, 30, 10, 0));
        given(publicRoomService.updateProgress(1L, 2L, 7L)).willReturn(response);

        mockMvc.perform(put("/api/public-rooms/2/progress").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"passageId\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.progressRate").value(40));

        verify(publicRoomService).updateProgress(1L, 2L, 7L);
    }

    @Test
    @DisplayName("새 댓글 수 요청의 현재 문단을 서비스에 전달한다")
    void countNewComments() throws Exception {
        givenValidMember(1L);
        given(commentService.countNewPublicRoomComments(1L, 2L, 7L)).willReturn(new NewCommentCountResponse(3));

        mockMvc.perform(get("/api/public-rooms/2/comments/new-count").header("X-Member-Id", "1")
                        .queryParam("currentPassageId", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.newCommentCount").value(3));

        verify(commentService).countNewPublicRoomComments(1L, 2L, 7L);
    }

    @Test
    @DisplayName("댓글 문장 목록 요청의 현재 문단을 서비스에 전달한다")
    void findCommentedSentences() throws Exception {
        givenValidMember(1L);
        given(commentService.findPublicRoomCommentedSentences(1L, 2L, 7L))
                .willReturn(new CommentedSentencesResponse(List.of()));

        mockMvc.perform(get("/api/public-rooms/2/commented-sentences").header("X-Member-Id", "1")
                        .queryParam("currentPassageId", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.commentedSentences").isEmpty());

        verify(commentService).findPublicRoomCommentedSentences(1L, 2L, 7L);
    }

    @Test
    @DisplayName("댓글 상세 확인 요청을 서비스에 전달한다")
    void findCommentDetails() throws Exception {
        givenValidMember(1L);
        given(commentService.findPublicRoomComments(1L, 2L, 8L)).willReturn(new CommentsResponse(List.of()));

        mockMvc.perform(post("/api/public-rooms/2/sentences/8/comment-detail-views")
                        .header("X-Member-Id", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comments").isEmpty());

        verify(commentService).findPublicRoomComments(1L, 2L, 8L);
    }

    @Test
    @DisplayName("공개방 댓글 작성 요청을 서비스에 전달하고 201을 반환한다")
    void createComment() throws Exception {
        givenValidMember(1L);
        var response = new CommentResponse(9L, 1L, "독자", "댓글", LocalDateTime.of(2026, 9, 30, 10, 0), null, true);
        given(commentService.createInPublicRoom(1L, 2L, 8L, new CommentContent("댓글"))).willReturn(response);

        mockMvc.perform(post("/api/public-rooms/2/sentences/8/comments").header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.commentId").value(9));

        verify(commentService).createInPublicRoom(1L, 2L, 8L, new CommentContent("댓글"));
    }

    private ClubBookResponse book() {
        return new ClubBookResponse(5L, "책", List.of("작가"), null, 10, BookStatus.ACTIVE);
    }
}
