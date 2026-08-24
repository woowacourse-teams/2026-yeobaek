package yeobaek.backend.club.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.LastReadingResponse;
import yeobaek.backend.club.dto.ProgressResponse;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

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
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        ClubMember clubMember = clubMemberRepository.findActiveByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PASSAGE_NOT_FOUND));
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
