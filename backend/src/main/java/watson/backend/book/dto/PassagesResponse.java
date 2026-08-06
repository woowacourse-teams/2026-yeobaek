package watson.backend.book.dto;

import java.util.List;

public record PassagesResponse(List<PassageResponse> passages) {

    public PassagesResponse {
        passages = List.copyOf(passages);
    }
}
