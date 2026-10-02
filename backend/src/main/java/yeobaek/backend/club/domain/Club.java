package yeobaek.backend.club.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;

@Entity
@Table(name = "clubs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clubs_join_code", columnNames = "join_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false, unique = true, updatable = false)
    private Long spaceId;

    @Column(name = "space_kind", nullable = false, length = 64, updatable = false)
    private String spaceKind;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false, length = ClubName.MAX_LENGTH))
    private ClubName name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "join_code", nullable = false, length = 10))
    private JoinCode joinCode;

    public Club(Long spaceId, ClubName name, JoinCode joinCode) {
        if (spaceId == null) {
            throw new IllegalArgumentException("모임 공간은 필수입니다.");
        }
        if (name == null) {
            throw new IllegalArgumentException("모임 이름은 필수입니다.");
        }
        this.name = name;
        this.spaceId = spaceId;
        this.spaceKind = "CLUB";
        this.joinCode = joinCode;
    }

    public String getName() {
        return name.value();
    }

    public String getJoinCode() {
        return joinCode.value();
    }

}
