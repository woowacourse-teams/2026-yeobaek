package yeobaek.backend.book.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import yeobaek.backend.book.domain.vo.BookGenres;

@Converter
public class BookGenresConverter implements AttributeConverter<BookGenres, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<String>> GENRES_TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(BookGenres attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute.value());
    }

    @Override
    public BookGenres convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new BookGenres(MAPPER.readValue(dbData, GENRES_TYPE));
    }
}
