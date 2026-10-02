package yeobaek.backend.collaboration.persistence;

import java.io.Serializable;

public record SpaceContentBindingId(Long spaceId, Long contentId) implements Serializable {
}
