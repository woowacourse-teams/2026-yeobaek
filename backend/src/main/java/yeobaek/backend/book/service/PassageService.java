package yeobaek.backend.book.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.dto.PassageResponse;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.dto.SentenceResponse;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.SentenceCommentCount;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassageService {

    private static final int MAX_RANGE_SIZE = 100;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final PassageRepository passageRepository;
    private final CommentRepository commentRepository;

    public PassagesResponse findPassages(Long memberId, Long clubId, int from, int to) {
        validateRange(from, to);
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        if (!clubMemberRepository.existsJoinedByMemberIdAndClubId(memberId, clubId)) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        }
        club.ensureBookAvailable();
        List<Passage> passages = passageRepository.findRangeByBookId(club.getBook().getId(), from, to);
        List<Long> sentenceIds = passages.stream()
                .flatMap(passage -> passage.getSentences().stream())
                .map(sentence -> sentence.getId())
                .toList();
        Map<Long, Long> commentCounts = countComments(memberId, clubId, sentenceIds);
        return new PassagesResponse(passages.stream()
                .map(passage -> new PassageResponse(passage.getId(), passage.getSequence(),
                        passage.getChapter().getId(), passage.getSentences().stream()
                        .map(sentence -> new SentenceResponse(sentence.getId(), sentence.getSequence(),
                                sentence.getContent(), commentCounts.getOrDefault(sentence.getId(), 0L)))
                        .toList()))
                .toList());
    }

    private Map<Long, Long> countComments(Long memberId, Long clubId, List<Long> sentenceIds) {
        if (sentenceIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countVisibleByMemberIdAndClubIdAndSentenceIdIn(memberId, clubId, sentenceIds).stream()
                .collect(Collectors.toMap(SentenceCommentCount::getSentenceId,
                        SentenceCommentCount::getCommentCount));
    }

    private void validateRange(int from, int to) {
        if (from < 1 || to < from) {
            throw new IllegalArgumentException("본문 범위가 올바르지 않습니다.");
        }
        if (to - from + 1 > MAX_RANGE_SIZE) {
            throw new IllegalArgumentException("본문은 한 번에 최대 " + MAX_RANGE_SIZE + "개까지 조회할 수 있습니다.");
        }
    }
}
