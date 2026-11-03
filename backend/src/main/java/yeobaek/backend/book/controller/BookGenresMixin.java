package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.BookGenres;

@JacksonMixin(BookGenres.class)
public abstract class BookGenresMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public BookGenresMixin(List<String> value) {
    }

    @JsonValue
    public abstract List<String> value();
}
