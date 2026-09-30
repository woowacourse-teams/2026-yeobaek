package yeobaek.backend.publicroom.dto;

import jakarta.validation.constraints.NotNull;

public record PublicRoomProgressUpdateRequest(@NotNull Long passageId) {
}
