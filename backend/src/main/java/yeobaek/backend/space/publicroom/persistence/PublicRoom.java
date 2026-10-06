package yeobaek.backend.space.publicroom.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.space.api.SpaceKind;

@Entity(name = "PublicRoom")
@Table(name = "public_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false, unique = true, updatable = false)
    private Long spaceId;

    @jakarta.persistence.Column(name = "space_kind", nullable = false, length = 64, updatable = false)
    private String spaceKind;

    public static PublicRoom create(Long spaceId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("공개방 공간은 필수입니다.");
        }
        PublicRoom room = new PublicRoom();
        room.spaceId = spaceId;
        room.spaceKind = SpaceKind.PUBLIC_ROOM.value();
        return room;
    }

}
