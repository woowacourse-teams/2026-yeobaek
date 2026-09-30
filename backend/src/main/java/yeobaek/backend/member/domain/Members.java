package yeobaek.backend.member.domain;

import java.util.List;

public final class Members {

    private final List<Member> values;

    public Members(List<Member> values) {
        this.values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<Long> ids() {
        return values.stream()
                .map(Member::getId)
                .toList();
    }

    public List<Member> asList() {
        return values;
    }
}
