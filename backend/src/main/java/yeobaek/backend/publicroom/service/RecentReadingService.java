package yeobaek.backend.publicroom.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoomActivity;
import yeobaek.backend.publicroom.dto.ClubReadingSpaceResponse;
import yeobaek.backend.publicroom.dto.PublicRoomReadingSpaceResponse;
import yeobaek.backend.publicroom.dto.RecentReadingResponse;
import yeobaek.backend.publicroom.repository.PublicRoomActivityRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentReadingService {

    private final ClubMemberRepository clubMemberRepository;
    private final PublicRoomActivityRepository activityRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    public Optional<RecentReadingResponse> findRecent(Long memberId) {
        ClubMember clubReading = clubMemberRepository.findAllJoinedWithLastReadingByMemberId(memberId).stream()
                .findFirst().orElse(null);
        PublicRoomActivity publicReading = activityRepository.findReadingsByMemberId(memberId).stream()
                .findFirst().orElse(null);
        if (clubReading == null && publicReading == null) {
            return Optional.empty();
        }
        if (publicReading == null || clubReading != null
                && !publicReading.getLastReadAt().isAfter(clubReading.getLastReadAt())) {
            return Optional.of(toClubResponse(clubReading));
        }
        return Optional.of(toPublicRoomResponse(publicReading));
    }

    private RecentReadingResponse toClubResponse(ClubMember reading) {
        Club club = reading.getClub();
        Book book = club.getBook();
        return new RecentReadingResponse(new ClubReadingSpaceResponse(club.getId(), club.getName()),
                toBook(book), reading.getLastReadPassage().getSequence().value(),
                reading.progressRate(), reading.getLastReadAt());
    }

    private RecentReadingResponse toPublicRoomResponse(PublicRoomActivity reading) {
        Book book = reading.getPublicRoom().getBook();
        return new RecentReadingResponse(new PublicRoomReadingSpaceResponse(reading.getPublicRoom().getId()),
                toBook(book), reading.getLastReadPassage().getSequence().value(),
                reading.progressRate(), reading.getLastReadAt());
    }

    private ClubBookResponse toBook(Book book) {
        List<String> authors = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getName().value())
                .toList();
        return ClubBookResponse.of(book, authors, bookCoverUrlResolver.resolve(book.getCoverImageKey()));
    }
}
