package yeobaek.backend.book.domain.vo;

import java.util.List;

public record BookGenres(List<String> value) {

    public BookGenres {
        value = List.copyOf(value);
    }
}
