package yeobaek.backend.club.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("모임을 생성하면 201과 참여 코드를 응답한다")
    void createClub() throws Exception {
        givenValidMember(1L);
        given(clubService.create(anyLong(), anyString(), anyLong())).willReturn(new ClubCreateResponse(
                1L, "교환독서 1기", "A3F9KQ",
                new ClubBookResponse(1L, "운수 좋은 날", List.of("현진건"), 312)));

        mockMvc.perform(post("/api/clubs")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"교환독서 1기\", \"bookId\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.joinCode").value("A3F9KQ"))
                .andExpect(jsonPath("$.book.bookId").value(1))
                .andDo(document("club-create", resource(ResourceSnippetParameters.builder()
                        .tag("모임")
                        .summary("모임 생성")
                        .description("책 한 권을 골라 모임을 만든다. 생성자는 자동으로 모임에 참여되고 참여 코드가 발급된다.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .requestFields(
                                PayloadDocumentation.fieldWithPath("name").description("모임 이름 (1~20자, 공백만은 불가)"),
                                PayloadDocumentation.fieldWithPath("bookId").description("읽을 도서 ID (영구 고정)"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("clubId").description("모임 ID"),
                                PayloadDocumentation.fieldWithPath("name").description("모임 이름"),
                                PayloadDocumentation.fieldWithPath("joinCode").description("참여 코드 (6자 대문자·숫자, 전역 unique, 영구 고정)"),
                                PayloadDocumentation.fieldWithPath("book.bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("book.title").description("도서 제목"),
                                PayloadDocumentation.fieldWithPath("book.authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("book.passageCount").description("본문 개수"))
                        .build())));
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
                .andExpect(jsonPath("$.clubId").value(1))
                .andDo(document("club-join", resource(ResourceSnippetParameters.builder()
                        .tag("모임")
                        .summary("참여 코드로 모임 참여")
                        .description("존재하지 않는 코드는 400(JOIN_CODE_NOT_FOUND). 이미 참여한 모임이면 같은 응답을 반환한다(멱등).")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .requestFields(PayloadDocumentation.fieldWithPath("joinCode").description("참여 코드"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("clubId").description("모임 ID"),
                                PayloadDocumentation.fieldWithPath("name").description("모임 이름"),
                                PayloadDocumentation.fieldWithPath("book.bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("book.title").description("도서 제목"),
                                PayloadDocumentation.fieldWithPath("book.authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("book.passageCount").description("본문 개수"))
                        .build())));
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
                .andExpect(jsonPath("$.clubs[1].myProgress").isEmpty())
                .andDo(document("club-my-list", resource(ResourceSnippetParameters.builder()
                        .tag("모임")
                        .summary("내 모임 목록 조회")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("clubs[].clubId").description("모임 ID"),
                                PayloadDocumentation.fieldWithPath("clubs[].name").description("모임 이름"),
                                PayloadDocumentation.fieldWithPath("clubs[].memberCount").description("모임 회원 수"),
                                PayloadDocumentation.fieldWithPath("clubs[].book.bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("clubs[].book.title").description("도서 제목"),
                                PayloadDocumentation.fieldWithPath("clubs[].book.authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("clubs[].book.passageCount").description("본문 개수"),
                                PayloadDocumentation.fieldWithPath("clubs[].myProgress").description("내 진도 (읽기 시작 전이면 null)").optional(),
                                PayloadDocumentation.fieldWithPath("clubs[].myProgress.lastReadPassageSequence").description("최근 열람 본문의 전체 순서").optional(),
                                PayloadDocumentation.fieldWithPath("clubs[].myProgress.progressRate").description("진도율 (0~100 정수, 반올림)").optional(),
                                PayloadDocumentation.fieldWithPath("clubs[].myProgress.lastReadAt").description("마지막으로 읽은 시간").optional())
                        .build())));
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
                .andExpect(jsonPath("$.members[0].mine").value(true))
                .andDo(document("club-detail",
                        pathParameters(parameterWithName("clubId").description("모임 ID")),
                        resource(ResourceSnippetParameters.builder()
                        .tag("모임")
                        .summary("모임 상세 조회")
                        .description("모임 상세 화면용: 초대 코드, 참여자 목록(참여 시각 오름차순), 내 진도. 모임 미소속은 403(NOT_CLUB_MEMBER).")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("clubId").description("모임 ID"),
                                PayloadDocumentation.fieldWithPath("name").description("모임 이름"),
                                PayloadDocumentation.fieldWithPath("joinCode").description("참여 코드 (전역 unique, 영구 고정)"),
                                PayloadDocumentation.fieldWithPath("book.bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("book.title").description("도서 제목"),
                                PayloadDocumentation.fieldWithPath("book.authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("book.passageCount").description("본문 개수"),
                                PayloadDocumentation.fieldWithPath("myProgress").description("내 진도 (읽기 시작 전이면 null)").optional(),
                                PayloadDocumentation.fieldWithPath("myProgress.lastReadPassageSequence").description("최근 열람 본문의 전체 순서").optional(),
                                PayloadDocumentation.fieldWithPath("myProgress.progressRate").description("진도율 (0~100 정수, 반올림)").optional(),
                                PayloadDocumentation.fieldWithPath("myProgress.lastReadAt").description("마지막으로 읽은 시간").optional(),
                                PayloadDocumentation.fieldWithPath("members[].memberId").description("회원 ID"),
                                PayloadDocumentation.fieldWithPath("members[].nickname").description("닉네임"),
                                PayloadDocumentation.fieldWithPath("members[].mine").description("요청자 본인 여부"))
                        .build())));
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
