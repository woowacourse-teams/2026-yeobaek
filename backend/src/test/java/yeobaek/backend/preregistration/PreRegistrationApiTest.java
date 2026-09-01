package yeobaek.backend.preregistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.preregistration.repository.PreRegistrationRepository;
import yeobaek.backend.support.IntegrationTest;

class PreRegistrationApiTest extends IntegrationTest {

    private static final String LANDING_ORIGIN = "https://yeobaek-landing-hypothesis.vercel.app";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreRegistrationRepository preRegistrationRepository;

    @Test
    @DisplayName("공개 API는 이메일을 한 번 저장하고 정규화 중복에 409를 반환한다")
    void createAndRejectNormalizedDuplicate() throws Exception {
        mockMvc.perform(post("/api/pre-registrations")
                        .header("Origin", LANDING_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"  Reader@Example.COM  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", LANDING_ORIGIN));

        mockMvc.perform(post("/api/pre-registrations")
                        .header("Origin", LANDING_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reader@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRE_REGISTRATION_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 사전신청한 이메일입니다."));

        assertThat(preRegistrationRepository.count()).isEqualTo(1);
        assertThat(preRegistrationRepository.existsByEmail("reader@example.com")).isTrue();
    }
}
