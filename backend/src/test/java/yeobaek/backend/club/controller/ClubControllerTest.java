package yeobaek.backend.club.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.ClubCreateResponse;
import yeobaek.backend.club.dto.ClubDetailResponse;
import yeobaek.backend.club.dto.ClubJoinResponse;
import yeobaek.backend.club.dto.ClubMemberResponse;
import yeobaek.backend.club.dto.MyClubResponse;
import yeobaek.backend.club.dto.MyClubsResponse;
import yeobaek.backend.club.dto.MyProgressResponse;
import yeobaek.backend.club.service.ClubService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@WebMvcTest(ClubController.class)
class ClubControllerTest extends ControllerTest {

    @MockitoBean
    private ClubService clubService;

    @Test
    @DisplayName("모임 생성 요청을 서비스에 전달하고 전체 응답 계약을 반환한다")
    void createClub() throws Exception {
        givenValidMember(1L);
        var response = new ClubCreateResponse(
                1L,
                "교환독서 1기",
                "A3F9KQ",
                new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312));
        given(clubService.create(1L, "교환독서 1기", 1L)).willReturn(response);

        mockMvc.perform(post("/api/clubs")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"교환독서 1기","bookId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubId").value(1))
                .andExpect(jsonPath("$.name").value("교환독서 1기"))
                .andExpect(jsonPath("$.joinCode").value("A3F9KQ"))
                .andExpect(jsonPath("$.book").isMap())
                .andExpect(jsonPath("$.book.bookId").value(1))
                .andExpect(jsonPath("$.book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.book.authors").isArray())
                .andExpect(jsonPath("$.book.authors.length()").value(1))
                .andExpect(jsonPath("$.book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.book.passageCount").value(312));

        verify(clubService, times(1)).create(1L, "교환독서 1기", 1L);
    }

    @Test
    @DisplayName("참여 코드로 모임에 참여한다")
    void joinClub() throws Exception {
        givenValidMember(1L);
        given(clubService.join(anyLong(), anyString())).willReturn(new ClubJoinResponse(
                1L, "교환독서 1기", new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312)));

        mockMvc.perform(post("/api/clubs/join")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\": \"A3F9KQ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubId").value(1));
    }

    @Test
    @DisplayName("존재하지 않는 참여 코드는 400과 JOIN_CODE_NOT_FOUND 코드를 응답한다")
    void joinWithUnknownCode() throws Exception {
        givenValidMember(1L);
        given(clubService.join(anyLong(), anyString()))
                .willThrow(new NotFoundException(ErrorCode.JOIN_CODE_NOT_FOUND));

        mockMvc.perform(post("/api/clubs/join")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\": \"NOCODE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JOIN_CODE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 참여 코드입니다."));
    }

    @Test
    @DisplayName("내 모임 목록을 조회한다")
    void findMyClubs() throws Exception {
        givenValidMember(1L);
        given(clubService.findMyClubs(1L)).willReturn(new MyClubsResponse(List.of(
                new MyClubResponse(1L, "교환독서 1기", 4,
                        new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312),
                        new MyProgressResponse(42, 13, LocalDateTime.of(2026, 8, 5, 14, 30))),
                new MyClubResponse(2L, "새 모임", 1,
                        new ClubBookResponse(2L, "무정", List.of("이광수"), 200), null))));

        mockMvc.perform(get("/api/clubs").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs[0].memberCount").value(4))
                .andExpect(jsonPath("$.clubs[0].myProgress.progressRate").value(13))
                .andExpect(jsonPath("$.clubs[1].myProgress").isEmpty());
    }

    @Test
    @DisplayName("모임 상세를 조회한다")
    void findClubDetail() throws Exception {
        givenValidMember(1L);
        given(clubService.findDetail(1L, 1L)).willReturn(new ClubDetailResponse(
                1L, "교환독서 1기", "A3F9KQ",
                new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312),
                new MyProgressResponse(42, 13, LocalDateTime.of(2026, 8, 5, 14, 30)),
                List.of(new ClubMemberResponse(1L, "민서", true), new ClubMemberResponse(2L, "지수", false))));

        mockMvc.perform(get("/api/clubs/{clubId}", 1L).header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinCode").value("A3F9KQ"))
                .andExpect(jsonPath("$.members[0].mine").value(true));
    }

    @Test
    @DisplayName("모임 미소속 회원의 상세 조회는 403을 응답한다")
    void rejectDetailForOutsider() throws Exception {
        givenValidMember(1L);
        given(clubService.findDetail(anyLong(), anyLong()))
                .willThrow(new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));

        mockMvc.perform(get("/api/clubs/1").header("X-Member-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CLUB_MEMBER"));
    }
}
