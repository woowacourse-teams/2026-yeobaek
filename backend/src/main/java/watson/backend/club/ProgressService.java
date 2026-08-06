package watson.backend.club;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.book.AuthorBookRepository;
import watson.backend.book.Book;
import watson.backend.book.Passage;
import watson.backend.book.PassageRepository;
import watson.backend.support.ForbiddenException;
import watson.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PassageRepository passageRepository;
    private final AuthorBookRepository authorBookRepository;

    @Transactional
    public ProgressResponse updateProgress(Long memberId, Long clubId, Long passageId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 모임입니다."));
        ClubMember clubMember = clubMemberRepository.findByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException("모임에 참여하지 않은 회원입니다."));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 본문입니다."));
        if (!club.isReading(passage)) {
            throw new IllegalArgumentException("모임의 도서에 속하지 않는 본문입니다.");
        }
        clubMember.updateProgress(passage, LocalDateTime.now());
        return new ProgressResponse(passage.getSequence(), clubMember.progressRate(), clubMember.getLastReadAt());
    }

    @Transactional(readOnly = true)
    public Optional<LastReadingResponse> findLastReading(Long memberId) {
        List<ClubMember> readings = clubMemberRepository.findAllWithLastReadingByMemberId(memberId);
        if (readings.isEmpty()) {
            return Optional.empty();
        }
        ClubMember latest = readings.getFirst();
        Club club = latest.getClub();
        Book book = club.getBook();
        List<String> authors = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getName())
                .toList();
        return Optional.of(new LastReadingResponse(club.getId(), club.getName(),
                ClubBookResponse.of(book, authors),
                latest.getLastReadPassage().getSequence(), latest.progressRate(), latest.getLastReadAt()));
    }
}
