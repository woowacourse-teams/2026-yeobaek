package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.Isni;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.dto.ClubCreateRequest;
import yeobaek.backend.club.dto.ClubJoinRequest;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentCreateRequest;
import yeobaek.backend.comment.dto.CommentUpdateRequest;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.MemberCreateRequest;

@SpringJUnitConfig(ValueObjectJsonTest.JacksonConfiguration.class)
class ValueObjectJsonTest {

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackages = "yeobaek.backend")
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    static class JacksonConfiguration {
    }

    @Autowired
    private JsonMapper mapper;

    @ParameterizedTest
    @MethodSource("scalarValues")
    @DisplayName("웹 MixIn은 VO를 JSON 문자열로 직렬화하고 동일한 VO로 복원한다")
    void roundTrip(Object value, String scalar) {
        String json = mapper.writeValueAsString(value);

        assertThat(json).isEqualTo(mapper.writeValueAsString(scalar));
        assertThat(mapper.readValue(json, value.getClass())).isEqualTo(value);
        assertThatThrownBy(() -> mapper.readValue("\" \"", value.getClass()))
                .isInstanceOf(JacksonException.class);
    }

    private static Stream<Arguments> scalarValues() {
        return Stream.of(
                Arguments.of(new Nickname("독자"), "독자"),
                Arguments.of(new ClubName("함께 읽기"), "함께 읽기"),
                Arguments.of(new JoinCode("AB1234"), "AB1234"),
                Arguments.of(new CommentContent("감상"), "감상"),
                Arguments.of(new BookTitle("도서"), "도서"),
                Arguments.of(new Publisher("출판사"), "출판사"),
                Arguments.of(new AuthorName("작가"), "작가"),
                Arguments.of(new Isni("000000012345964X"), "000000012345964X"),
                Arguments.of(new ChapterTitle("1장"), "1장"),
                Arguments.of(new SentenceContent("  문장\n"), "  문장\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nickname\":null}", "{\"nickname\":\" \"}",
            "{\"nickname\":{\"value\":\"독자\"}}", "{\"nickname\":[]}"})
    @DisplayName("필수 VO의 누락·null·유효하지 않은 값과 객체 형태를 거부한다")
    void rejectInvalidRequiredValue(String json) {
        assertThatThrownBy(() -> mapper.readValue(json, MemberCreateRequest.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    @DisplayName("선택 VO의 누락과 null은 부재로 유지한다")
    void optionalValues() {
        var missing = mapper.readValue("{\"title\":\"도서\"}", BookUploadRequest.class);
        var explicitNull = mapper.readValue("{\"title\":\"도서\",\"publisher\":null}", BookUploadRequest.class);
        var existing = mapper.readValue("{\"authorId\":1}", AuthorEntryRequest.class);
        var newAuthor = mapper.readValue("{\"name\":\"작가\",\"isni\":null}", AuthorEntryRequest.class);

        assertThat(missing.publisher()).isNull();
        assertThat(explicitNull.publisher()).isNull();
        assertThat(existing.name()).isNull();
        assertThat(newAuthor.isni()).isNull();
    }

    @Test
    @DisplayName("ISNI 정규화는 역직렬화 중 VO 생성자가 수행한다")
    void normalizeIsni() {
        var request = mapper.readValue(
                "{\"name\":\"작가\",\"isni\":\"0000 0001-2345 964X\"}", AuthorEntryRequest.class);

        assertThat(request.isni()).isEqualTo(new Isni("000000012345964X"));
    }

    @ParameterizedTest
    @MethodSource("requiredFields")
    @DisplayName("모든 필수 VO 요청 필드는 누락과 null을 거부한다")
    void rejectMissingOrNull(Class<?> requestType, String field) {
        assertThatThrownBy(() -> mapper.readValue("{}", requestType))
                .isInstanceOf(JacksonException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"" + field + "\":null}", requestType))
                .isInstanceOf(JacksonException.class);
    }

    private static Stream<Arguments> requiredFields() {
        return Stream.of(
                Arguments.of(MemberCreateRequest.class, "nickname"),
                Arguments.of(ClubCreateRequest.class, "name"),
                Arguments.of(ClubJoinRequest.class, "joinCode"),
                Arguments.of(CommentCreateRequest.class, "content"),
                Arguments.of(CommentUpdateRequest.class, "content"),
                Arguments.of(BookUploadRequest.class, "title"),
                Arguments.of(ChapterUploadRequest.class, "title"),
                Arguments.of(SentenceUploadRequest.class, "content"),
                Arguments.of(AuthorEntryRequest.class, "name"));
    }
}
