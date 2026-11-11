package yeobaek.backend.support.monitoring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MonitoringGitHubClient {

    private static final int MAX_BYTES = 5_000_000;
    private static final int PAGE_SIZE = 100;
    private final MonitoringSettings settings;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final String baseUrl;

    @Autowired
    public MonitoringGitHubClient(MonitoringSettings settings, ObjectMapper mapper) {
        this(settings, mapper, "https://api.github.com");
    }

    MonitoringGitHubClient(MonitoringSettings settings, ObjectMapper mapper, String baseUrl) {
        this.settings = settings;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public JsonNode get(String path) {
        return json(request(path).GET().build());
    }

    public void post(String path, Object body) {
        json(request(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build());
    }

    public List<JsonNode> pages(String path, String field) {
        List<JsonNode> items = new ArrayList<>();
        String separator = path.contains("?") ? "&" : "?";
        for (int page = 1; page <= 100; page++) {
            JsonNode batch = get(path + separator + "per_page=100&page=" + page).path(field);
            if (!batch.isArray()) {
                throw new IllegalStateException("GitHub 조회 응답을 확인할 수 없습니다.");
            }
            batch.forEach(items::add);
            if (batch.size() < PAGE_SIZE) {
                return items;
            }
        }
        throw new IllegalStateException("GitHub 조회 범위를 초과했습니다.");
    }

    public JsonNode artifact(long id, String filename) {
        HttpResponse<InputStream> redirect = send(request("/actions/artifacts/" + id + "/zip").GET().build());
        try {
            redirect.body().close();
            if (redirect.statusCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
                throw new IllegalStateException("측정 결과가 만료됐거나 다운로드할 수 없습니다.");
            }
            URI location = URI.create(redirect.headers().firstValue("Location").orElseThrow());
            if (!"https".equals(location.getScheme())) {
                throw new IllegalStateException("측정 결과 다운로드 주소가 올바르지 않습니다.");
            }
            // The signed storage URL must never receive the GitHub credential.
            HttpResponse<InputStream> download = send(HttpRequest.newBuilder(location)
                    .timeout(Duration.ofSeconds(15)).GET().build());
            try (InputStream archive = download.body()) {
                requireSuccess(download.statusCode());
                return unzip(boundedRead(archive), filename);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("측정 결과를 읽을 수 없습니다.", exception);
        }
    }

    JsonNode unzip(byte[] archive, String filename) throws IOException {
        try (ZipInputStream zipped = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (ZipEntry entry = zipped.getNextEntry(); entry != null; entry = zipped.getNextEntry()) {
                if (filename.equals(entry.getName()) && !entry.isDirectory()) {
                    return mapper.readTree(boundedRead(zipped));
                }
            }
        }
        throw new IllegalStateException("측정 결과 파일을 찾을 수 없습니다.");
    }

    private HttpRequest.Builder request(String path) {
        settings.requireConfigured();
        return HttpRequest.newBuilder(URI.create(baseUrl + "/repos/" + settings.getRepository() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + settings.getToken())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
    }

    private JsonNode json(HttpRequest request) {
        HttpResponse<InputStream> response = send(request);
        try (InputStream body = response.body()) {
            requireSuccess(response.statusCode());
            byte[] bytes = boundedRead(body);
            return bytes.length == 0 ? mapper.createObjectNode() : mapper.readTree(bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub 응답을 읽을 수 없습니다.", exception);
        }
    }

    private HttpResponse<InputStream> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub 조회가 중단됐습니다.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub에 연결할 수 없습니다.", exception);
        }
    }

    private void requireSuccess(int status) {
        if (status < 200 || status >= 300) {
            // Do not expose upstream bodies, credentials, or signed URLs through the global handler.
            throw new IllegalStateException("GitHub 요청에 실패했습니다. 권한·설정·사용량을 확인하세요. HTTP " + status);
        }
    }

    private byte[] boundedRead(InputStream stream) throws IOException {
        byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) {
            throw new IllegalStateException("측정 결과가 허용 크기를 초과했습니다.");
        }
        return bytes;
    }
}
