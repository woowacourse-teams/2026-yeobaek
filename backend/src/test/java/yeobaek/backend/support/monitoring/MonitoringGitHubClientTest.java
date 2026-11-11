package yeobaek.backend.support.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import yeobaek.backend.support.LogCapture;

class MonitoringGitHubClientTest {

    @Test
    @DisplayName("서버 토큰으로 페이지를 순회해 전체 목록을 읽는다")
    void retrievesAllPagesWithServerSideToken() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/repos/owner/repo/actions/artifacts", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");
            String items = exchange.getRequestURI().getQuery().endsWith("&page=1")
                    ? String.join(",", java.util.Collections.nCopies(100, "{\"id\":1}")) : "{\"id\":2}";
            byte[] response = ("{\"artifacts\":[" + items + "]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try (var logs = new LogCapture(MonitoringGitHubClient.class.getName())) {
            var client = client("http://localhost:" + server.getAddress().getPort());
            assertThat(client.pages("/actions/artifacts", "artifacts")).hasSize(101);

            var event = logs.event("monitoring.github.pages", "success");
            assertThat(logs.field(event, "resultCount")).isEqualTo(101);
            assertThat(logs.structuredText()).doesNotContain("test-token", "Authorization");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("압축 파일에서 요청한 결과 JSON만 읽는다")
    void readsOnlyExpectedResultFileWithoutExtractingToDisk() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(buffer)) {
            archive.putNextEntry(new ZipEntry("../other.json"));
            archive.write("{}".getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
            archive.putNextEntry(new ZipEntry("result.json"));
            archive.write("{\"summary\":{\"failures\":2}}".getBytes(StandardCharsets.UTF_8));
        }
        assertThat(client("https://api.github.com").unzip(buffer.toByteArray(), "result.json")
                .path("summary").path("failures").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("압축 해제 후 크기 제한을 넘는 결과는 거부한다")
    void rejectsOversizedUncompressedArtifact() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(buffer)) {
            archive.putNextEntry(new ZipEntry("result.json"));
            archive.write(new byte[5_000_001]);
        }
        var client = client("https://api.github.com");
        byte[] compressed = buffer.toByteArray();
        assertThatThrownBy(() -> client.unzip(compressed, "result.json"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("크기");
    }

    @Test
    @DisplayName("잘못된 JSON 오류에는 응답 원문 없이 오류 유형과 발생 위치만 보존한다")
    void hidesResponseBodyInParsingFailure() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(buffer)) {
            archive.putNextEntry(new ZipEntry("result.json"));
            archive.write("{private-response-token".getBytes(StandardCharsets.UTF_8));
        }
        var client = client("https://api.github.com");
        byte[] compressed = buffer.toByteArray();

        assertThatThrownBy(() -> client.unzip(compressed, "result.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GitHub 응답 JSON 형식이 올바르지 않습니다.")
                .satisfies(exception -> {
                    assertThat(exception.getCause().getStackTrace()).isNotEmpty();
                    assertThat(exception.getCause().getMessage()).contains("Exception").doesNotContain("private-response-token");
                });
    }

    private MonitoringGitHubClient client(String baseUrl) {
        return new MonitoringGitHubClient(new MonitoringSettings("owner/repo", "test-token", "develop", "dev",
                "backend-dev.yml"), new ObjectMapper(), baseUrl);
    }
}
