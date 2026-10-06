package yeobaek.backend.application.club;

import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.space.api.club.ClubResponse;

public record ClubCommandResult(ClubResponse club, ContentCardResult content) {
}
