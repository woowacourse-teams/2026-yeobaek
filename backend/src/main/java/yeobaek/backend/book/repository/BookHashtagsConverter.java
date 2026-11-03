package yeobaek.backend.book.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import yeobaek.backend.book.domain.vo.BookHashtags;

@Converter
public class BookHashtagsConverter implements AttributeConverter<BookHashtags, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<String>> HASHTAGS_TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(BookHashtags attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute.value());
    }

    @Override
    public BookHashtags convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new BookHashtags(MAPPER.readValue(dbData, HASHTAGS_TYPE));
    }
}
