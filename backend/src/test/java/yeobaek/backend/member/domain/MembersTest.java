package yeobaek.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.member.domain.vo.Nickname;

class MembersTest {

    @Test
    @DisplayName("회원이 없으면 빈 컬렉션이다")
    void isEmpty() {
        assertThat(new Members(List.of()).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("회원 id를 순서대로 반환한다")
    void ids() {
        Members members = new Members(List.of(memberWithId(2L, "둘째"), memberWithId(1L, "첫째")));

        assertThat(members.ids()).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("원본 목록을 복사하고 변경할 수 없는 회원 목록을 반환한다")
    void copyAndExposeUnmodifiableList() {
        Member member = memberWithId(1L, "첫째");
        List<Member> source = new ArrayList<>(List.of(member));
        Members members = new Members(source);

        source.clear();

        assertThat(members.asList()).containsExactly(member);
        assertThatThrownBy(() -> members.asList().add(member))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Member memberWithId(Long id, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
