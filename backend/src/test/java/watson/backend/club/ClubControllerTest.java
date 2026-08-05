package watson.backend.club;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.support.ControllerTest;

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
                                PayloadDocumentation.fieldWithPath("name").description("모임 이름"),
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
}
