package watson.backend.book.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.book.domain.Passage;
import watson.backend.book.dto.PassageResponse;
import watson.backend.book.dto.PassagesResponse;
import watson.backend.book.repository.PassageRepository;
import watson.backend.club.domain.Club;
import watson.backend.club.repository.ClubMemberRepository;
import watson.backend.club.repository.ClubRepository;
import watson.backend.comment.repository.CommentRepository;
import watson.backend.comment.repository.PassageCommentCount;
import watson.backend.support.ErrorCode;
import watson.backend.support.ForbiddenException;
import watson.backend.support.NotFoundException;

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
        if (!clubMemberRepository.existsByMemberIdAndClubId(memberId, clubId)) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER);
        }
        List<Passage> passages = passageRepository.findRangeByBookId(club.getBook().getId(), from, to);
        Map<Long, Long> commentCounts = commentRepository.countByClubIdAndSequenceRange(clubId, from, to).stream()
                .collect(Collectors.toMap(PassageCommentCount::getPassageId, PassageCommentCount::getCommentCount));
        return new PassagesResponse(passages.stream()
                .map(passage -> new PassageResponse(passage.getId(), passage.getSequence(),
                        passage.getChapter().getId(), passage.getContent(),
                        commentCounts.getOrDefault(passage.getId(), 0L)))
                .toList());
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
