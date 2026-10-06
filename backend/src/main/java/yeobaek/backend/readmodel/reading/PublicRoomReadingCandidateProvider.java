package yeobaek.backend.readmodel.reading;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;

@Component
@RequiredArgsConstructor
public class PublicRoomReadingCandidateProvider implements ReadingCandidateProvider {

    private final PublicRoomApi rooms;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public List<Candidate> findCandidates(MemberId actorId) {
        return rooms.findAll().stream().map(room -> new Candidate(room, null)).toList();
    }
}
