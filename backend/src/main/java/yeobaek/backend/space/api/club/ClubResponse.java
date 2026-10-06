package yeobaek.backend.space.api.club;

import yeobaek.backend.space.api.Space;

public interface ClubResponse extends Space {

    Long clubId();

    String name();

    String joinCode();
}
