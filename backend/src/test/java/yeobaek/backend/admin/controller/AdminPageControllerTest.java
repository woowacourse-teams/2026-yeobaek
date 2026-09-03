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

    @Test
    @DisplayName("신규 도서 표지는 파일 선택과 드래그 앤 드롭으로 첨부할 수 있다")
    void serveBookCoverDropUi() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"new-cover-drop-zone\"")))
                .andExpect(content().string(containsString("이미지를 여기로 끌어다 놓거나 클릭해서 선택하세요.")))
                .andExpect(content().string(containsString("addEventListener('dragover'")))
                .andExpect(content().string(containsString("addEventListener('drop'")))
                .andExpect(content().string(containsString("event.dataTransfer.files[0]")))
                .andExpect(content().string(containsString("droppedNewCoverFile = null")))
                .andExpect(content().string(containsString("droppedNewCoverFile ?? newCoverFileInput.files[0]")))
                .andExpect(content().string(containsString("payload.coverImageKey = await uploadCover(coverFile)")));
    }

    @Test
    @DisplayName("도서 JSON은 파일 선택과 드래그 앤 드롭으로 입력할 수 있다")
    void serveBookJsonDropUi() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"upload-json-drop-zone\"")))
                .andExpect(content().string(containsString("id=\"upload-json-file\"")))
                .andExpect(content().string(containsString("accept=\".json,application/json\"")))
                .andExpect(content().string(containsString("id=\"upload-json-file-name\"")))
                .andExpect(content().string(containsString("id=\"upload-result\" class=\"result\" aria-live=\"polite\"")))
                .andExpect(content().string(containsString("uploadJsonTextarea.addEventListener('input'")))
                .andExpect(content().string(containsString("uploadJsonFileInput.addEventListener('change'")))
                .andExpect(content().string(containsString("loadJsonFile(file)")))
                .andExpect(content().string(containsString("uploadJsonDropZone.addEventListener('dragover'")))
                .andExpect(content().string(containsString("uploadJsonDropZone.addEventListener('drop'")))
                .andExpect(content().string(containsString("await file.text()")))
                .andExpect(content().string(containsString("loadSequence !== jsonFileLoadSequence")))
                .andExpect(content().string(containsString("uploadJsonTextarea.value = fileContent")))
                .andExpect(content().string(containsString("uploadJsonFileName.textContent = file.name")))
                .andExpect(content().string(containsString("file.name.toLowerCase().endsWith('.json')")))
                .andExpect(content().string(containsString("JSON 파일만 첨부할 수 있습니다.")))
                .andExpect(content().string(containsString("JSON 파일을 불러왔습니다.")));
    }
}
