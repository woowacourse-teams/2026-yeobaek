package yeobaek.backend.admin.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.support.IntegrationTest;

class AdminPageControllerTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("관리자 페이지는 삭제 상태와 도서 삭제 동작을 제공한다")
    void serveBookDeleteUi() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("삭제됨")))
                .andExpect(content().string(containsString("book.status === 'DELETED'")))
                .andExpect(content().string(containsString("data-book-id")))
                .andExpect(content().string(containsString("method: 'DELETE'")))
                .andExpect(content().string(containsString("도서를 삭제하시겠습니까?")));
    }
}
