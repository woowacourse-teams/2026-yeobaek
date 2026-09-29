package yeobaek.backend.club.service;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
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
import yeobaek.backend.support.InvalidRequestException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PassageRepository passageRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional
    public ProgressResponse updateProgress(Long memberId, Long clubId, Long passageId) {
        log.atInfo().addKeyValue(OPERATION, "progress.updateProgress")
                .addKeyValue(MEMBER_ID, memberId).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("passageId", passageId).log("진도를 갱신합니다.");
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "진도를 갱신할 모임이 존재하지 않습니다: clubId=" + clubId,
                        Map.of(MEMBER_ID, memberId.toString(), CLUB_ID, clubId.toString())));
        ClubMember clubMember = clubMemberRepository.findJoinedByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.NOT_CLUB_MEMBER,
                        "모임에 참여 중인 회원만 진도를 갱신할 수 있습니다: clubId=" + clubId,
                        Map.of(MEMBER_ID, memberId.toString(), CLUB_ID, clubId.toString())));
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.PASSAGE_NOT_FOUND,
                        "진도를 갱신할 본문이 존재하지 않습니다: passageId=" + passageId,
                        Map.of(MEMBER_ID, memberId.toString(), CLUB_ID, clubId.toString(),
                                "passageId", passageId.toString())));
        if (!club.isReading(passage)) {
            throw new InvalidRequestException(
                    "모임의 도서에 속하지 않는 본문입니다.",
                    Map.of(MEMBER_ID, memberId.toString(), CLUB_ID, clubId.toString(),
                            "passageId", passageId.toString()));
        }
        club.ensureBookAvailable();
        clubMember.updateProgress(passage, LocalDateTime.now());
        var response = new ProgressResponse(
                passage.getSequence().value(), clubMember.progressRate(), clubMember.getLastReadAt());
        log.atInfo().addKeyValue(OPERATION, "progress.updateProgress").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("progressRate", response.progressRate()).log("진도를 갱신했습니다.");
        return response;
    }

    @Transactional(readOnly = true)
    public Optional<LastReadingResponse> findLastReading(Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "progress.findLastReading")
                .addKeyValue(MEMBER_ID, memberId).log("최근 독서를 조회합니다.");
        List<ClubMember> readings = clubMemberRepository.findAllJoinedWithLastReadingByMemberId(memberId);
        if (readings.isEmpty()) {
            log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").addKeyValue(PHASE, SUCCESS)
                    .addKeyValue(MEMBER_ID, memberId).addKeyValue("found", false).log("최근 독서를 조회했습니다.");
            return Optional.empty();
        }
        ClubMember latest = readings.getFirst();
        Club club = latest.getClub();
        Book book = club.getBook();
        List<String> authors = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getName().value())
                .toList();
        var response = Optional.of(new LastReadingResponse(club.getId(), club.getName(),
                ClubBookResponse.of(book, authors, bookCoverUrlResolver.resolve(book.getCoverImageKey())),
                latest.getLastReadPassage().getSequence().value(), latest.progressRate(), latest.getLastReadAt()));
        log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue("found", true).addKeyValue(CLUB_ID, club.getId())
                .log("최근 독서를 조회했습니다.");
        return response;
    }
}
