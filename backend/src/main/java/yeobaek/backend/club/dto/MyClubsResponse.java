package yeobaek.backend.club.dto;

import java.util.List;

public record MyClubsResponse(List<MyClubResponse> clubs) {

    public MyClubsResponse {
        clubs = List.copyOf(clubs);
    }
}
