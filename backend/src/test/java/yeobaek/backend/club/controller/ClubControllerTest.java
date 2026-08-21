package yeobaek.backend.club.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
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

@WebMvcTest(ClubController.class)
class ClubControllerTest extends ControllerTest {

    @MockitoBean
    private ClubService clubService;

    @Test
    @DisplayName("모임 생성 요청을 서비스에 전달하고 전체 응답 계약을 반환한다")
    void createClub() throws Exception {
        givenValidMember(1L);
        var response = new ClubCreateResponse(
                10L,
                "교환독서 1기",
                "A3F9KQ",
                new ClubBookResponse(5L, "운수 좋은 날", List.of("현진건"), 312));
        given(clubService.create(1L, "교환독서 1기", 5L)).willReturn(response);

        mockMvc.perform(post("/api/clubs")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"교환독서 1기","bookId":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubId").value(10))
                .andExpect(jsonPath("$.name").value("교환독서 1기"))
                .andExpect(jsonPath("$.joinCode").value("A3F9KQ"))
                .andExpect(jsonPath("$.book").isMap())
                .andExpect(jsonPath("$.book.bookId").value(5))
                .andExpect(jsonPath("$.book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.book.authors").isArray())
                .andExpect(jsonPath("$.book.authors.length()").value(1))
                .andExpect(jsonPath("$.book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.book.passageCount").value(312));

        verify(clubService, times(1)).create(1L, "교환독서 1기", 5L);
    }

    @Test
    @DisplayName("참여 코드를 서비스에 전달하고 모임 전체 계약을 반환한다")
    void joinClub() throws Exception {
        givenValidMember(2L);
        var response = new ClubJoinResponse(
                10L,
                "교환독서 1기",
                new ClubBookResponse(5L, "운수 좋은 날", List.of("현진건", "공동 저자"), 312));
        given(clubService.join(2L, "A3F9KQ")).willReturn(response);

        mockMvc.perform(post("/api/clubs/join")
                        .header("X-Member-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"joinCode":"A3F9KQ"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubId").value(10))
                .andExpect(jsonPath("$.name").value("교환독서 1기"))
                .andExpect(jsonPath("$.book").isMap())
                .andExpect(jsonPath("$.book.bookId").value(5))
                .andExpect(jsonPath("$.book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.book.authors").isArray())
                .andExpect(jsonPath("$.book.authors.length()").value(2))
                .andExpect(jsonPath("$.book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.book.authors[1]").value("공동 저자"))
                .andExpect(jsonPath("$.book.passageCount").value(312));

        verify(clubService, times(1)).join(2L, "A3F9KQ");
    }

    @Test
    @DisplayName("내 모임 목록의 중첩 객체와 nullable 필드를 모두 반환한다")
    void findMyClubs() throws Exception {
        givenValidMember(3L);
        var lastReadAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        var book = new ClubBookResponse(5L, "운수 좋은 날", List.of("현진건"), 312);
        var response = new MyClubsResponse(List.of(
                new MyClubResponse(10L, "교환독서 1기", 4, book,
                        new MyProgressResponse(42, 13, lastReadAt)),
                new MyClubResponse(11L, "아직 읽기 전", 1, book, null)));
        given(clubService.findMyClubs(3L)).willReturn(response);

        mockMvc.perform(get("/api/clubs")
                        .header("X-Member-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubs").isArray())
                .andExpect(jsonPath("$.clubs.length()").value(2))
                .andExpect(jsonPath("$.clubs[0].clubId").value(10))
                .andExpect(jsonPath("$.clubs[0].name").value("교환독서 1기"))
                .andExpect(jsonPath("$.clubs[0].memberCount").value(4))
                .andExpect(jsonPath("$.clubs[0].book").isMap())
                .andExpect(jsonPath("$.clubs[0].book.bookId").value(5))
                .andExpect(jsonPath("$.clubs[0].book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.clubs[0].book.authors").isArray())
                .andExpect(jsonPath("$.clubs[0].book.authors.length()").value(1))
                .andExpect(jsonPath("$.clubs[0].book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.clubs[0].book.passageCount").value(312))
                .andExpect(jsonPath("$.clubs[0].myProgress").isMap())
                .andExpect(jsonPath("$.clubs[0].myProgress.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.clubs[0].myProgress.progressRate").value(13))
                .andExpect(jsonPath("$.clubs[0].myProgress.lastReadAt").value("2026-08-05T14:30:00"))
                .andExpect(jsonPath("$.clubs[1].clubId").value(11))
                .andExpect(jsonPath("$.clubs[1].name").value("아직 읽기 전"))
                .andExpect(jsonPath("$.clubs[1].memberCount").value(1))
                .andExpect(jsonPath("$.clubs[1].book").isMap())
                .andExpect(jsonPath("$.clubs[1].book.bookId").value(5))
                .andExpect(jsonPath("$.clubs[1].book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.clubs[1].book.authors").isArray())
                .andExpect(jsonPath("$.clubs[1].book.authors.length()").value(1))
                .andExpect(jsonPath("$.clubs[1].book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.clubs[1].book.passageCount").value(312))
                .andExpect(jsonPath("$.clubs[1].myProgress").value((Object) null));

        verify(clubService, times(1)).findMyClubs(3L);
    }

    @Test
    @DisplayName("모임 식별자를 서비스에 전달하고 상세 전체 계약을 반환한다")
    void findClubDetail() throws Exception {
        givenValidMember(4L);
        var lastReadAt = LocalDateTime.of(2026, 8, 6, 9, 15);
        var response = new ClubDetailResponse(
                10L,
                "교환독서 1기",
                "A3F9KQ",
                new ClubBookResponse(5L, "운수 좋은 날", List.of("현진건"), 312),
                new MyProgressResponse(42, 13, lastReadAt),
                List.of(
                        new ClubMemberResponse(4L, "민서", true),
                        new ClubMemberResponse(8L, "지수", false)));
        given(clubService.findDetail(4L, 10L)).willReturn(response);

        mockMvc.perform(get("/api/clubs/{clubId}", 10L)
                        .header("X-Member-Id", "4"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubId").value(10))
                .andExpect(jsonPath("$.name").value("교환독서 1기"))
                .andExpect(jsonPath("$.joinCode").value("A3F9KQ"))
                .andExpect(jsonPath("$.book").isMap())
                .andExpect(jsonPath("$.book.bookId").value(5))
                .andExpect(jsonPath("$.book.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.book.authors").isArray())
                .andExpect(jsonPath("$.book.authors.length()").value(1))
                .andExpect(jsonPath("$.book.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.book.passageCount").value(312))
                .andExpect(jsonPath("$.myProgress").isMap())
                .andExpect(jsonPath("$.myProgress.lastReadPassageSequence").value(42))
                .andExpect(jsonPath("$.myProgress.progressRate").value(13))
                .andExpect(jsonPath("$.myProgress.lastReadAt").value("2026-08-06T09:15:00"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].memberId").value(4))
                .andExpect(jsonPath("$.members[0].nickname").value("민서"))
                .andExpect(jsonPath("$.members[0].mine").value(true))
                .andExpect(jsonPath("$.members[1].memberId").value(8))
                .andExpect(jsonPath("$.members[1].nickname").value("지수"))
                .andExpect(jsonPath("$.members[1].mine").value(false));

        verify(clubService, times(1)).findDetail(4L, 10L);
    }

    @Test
    @DisplayName("모임 생성 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingCreateBody() throws Exception {
        givenValidMember(5L);

        mockMvc.perform(post("/api/clubs")
                        .header("X-Member-Id", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(clubService);
    }

    @Test
    @DisplayName("모임 참여 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingJoinBody() throws Exception {
        givenValidMember(6L);

        mockMvc.perform(post("/api/clubs/join")
                        .header("X-Member-Id", "6")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(clubService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(7L);
        var serviceException = new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        given(clubService.findDetail(7L, 999L)).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/clubs/{clubId}", 999L)
                        .header("X-Member-Id", "7"))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(clubService, times(1)).findDetail(7L, 999L);
    }
}
