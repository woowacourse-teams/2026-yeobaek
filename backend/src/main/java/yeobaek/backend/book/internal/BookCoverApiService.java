package yeobaek.backend.book.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.book.api.BookCoverApi;
import yeobaek.backend.book.service.BookCoverUrlResolver;

@Service
@RequiredArgsConstructor
public class BookCoverApiService implements BookCoverApi {

    private final BookCoverUrlResolver resolver;

    @Override
    public String resolve(String coverImageKey) {
        return resolver.resolve(coverImageKey);
    }
}
