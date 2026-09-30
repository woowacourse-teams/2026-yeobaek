package yeobaek.backend.club.domain;

import java.util.List;

public final class Clubs {

    private final List<Club> values;

    public Clubs(List<Club> values) {
        this.values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<Long> ids() {
        return values.stream()
                .map(Club::getId)
                .toList();
    }

    public List<Club> asList() {
        return values;
    }
}
