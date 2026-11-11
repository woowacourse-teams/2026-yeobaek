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
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MonitoringGitHubClientTest {

    @Test
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
        try {
            var client = client("http://localhost:" + server.getAddress().getPort());
            assertThat(client.pages("/actions/artifacts", "artifacts")).hasSize(101);
        } finally {
            server.stop(0);
        }
    }

    @Test
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

    private MonitoringGitHubClient client(String baseUrl) {
        return new MonitoringGitHubClient(new MonitoringSettings("owner/repo", "test-token", "develop", "dev",
                "backend-dev.yml"), new ObjectMapper(), baseUrl);
    }
}
