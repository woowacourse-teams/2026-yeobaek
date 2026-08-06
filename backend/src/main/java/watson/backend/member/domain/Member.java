package watson.backend.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    private static final int MAX_NICKNAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    public Member(String nickname) {
        validate(nickname);
        this.nickname = nickname;
    }

    private static void validate(String nickname) {
        if (nickname == null || nickname.isBlank() || nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("닉네임은 공백이 아닌 1~" + MAX_NICKNAME_LENGTH + "자여야 합니다.");
        }
    }

    // 생성자에서 예외가 발생할 수 있어 파이널라이저 공격에 노출될 수 있다.
    // finalize()를 오버라이드 불가능하게 막아 차단한다 (클래스를 final로 두면 Hibernate 지연 로딩 프록시 생성이 불가능해진다).
    @Override
    @SuppressWarnings({"deprecation", "removal"})
    protected final void finalize() {
    }
}
