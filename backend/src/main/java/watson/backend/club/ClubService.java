package watson.backend.club;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.book.AuthorBookRepository;
import watson.backend.book.Book;
import watson.backend.book.BookRepository;
import watson.backend.member.MemberRepository;
import watson.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class ClubService {

    private static final int MAX_JOIN_CODE_ATTEMPTS = 5;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final BookRepository bookRepository;
    private final AuthorBookRepository authorBookRepository;
    private final MemberRepository memberRepository;
    private final JoinCodeGenerator joinCodeGenerator;

    @Transactional
    public ClubCreateResponse create(Long memberId, String name, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 도서입니다."));
        Club club = clubRepository.save(new Club(name, book, generateUniqueJoinCode()));
        clubMemberRepository.save(new ClubMember(memberRepository.getReferenceById(memberId), club));
        return new ClubCreateResponse(club.getId(), club.getName(), club.getJoinCode(),
                ClubBookResponse.of(book, authorNames(book)));
    }

    @Transactional
    public ClubJoinResponse join(Long memberId, String joinCode) {
        Club club = clubRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 참여 코드입니다."));
        if (!clubMemberRepository.existsByMemberIdAndClubId(memberId, club.getId())) {
            clubMemberRepository.save(new ClubMember(memberRepository.getReferenceById(memberId), club));
        }
        Book book = club.getBook();
        return new ClubJoinResponse(club.getId(), club.getName(), ClubBookResponse.of(book, authorNames(book)));
    }

    @Transactional(readOnly = true)
    public MyClubsResponse findMyClubs(Long memberId) {
        List<ClubMember> clubMembers = clubMemberRepository.findAllWithClubAndBookByMemberId(memberId);
        List<Long> clubIds = clubMembers.stream().map(clubMember -> clubMember.getClub().getId()).toList();
        Map<Long, Long> memberCounts = clubMemberRepository.countByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubMemberCount::getClubId, ClubMemberCount::getMemberCount));
        Map<Long, List<String>> authorNames = authorNamesByBookId(
                clubMembers.stream().map(clubMember -> clubMember.getClub().getBook().getId()).distinct().toList());
        return new MyClubsResponse(clubMembers.stream()
                .map(clubMember -> {
                    Club club = clubMember.getClub();
                    Book book = club.getBook();
                    return new MyClubResponse(club.getId(), club.getName(),
                            memberCounts.getOrDefault(club.getId(), 0L),
                            ClubBookResponse.of(book, authorNames.getOrDefault(book.getId(), List.of())),
                            toMyProgress(clubMember, book));
                })
                .toList());
    }

    private MyProgressResponse toMyProgress(ClubMember clubMember, Book book) {
        if (clubMember.getLastReadPassage() == null) {
            return null;
        }
        int sequence = clubMember.getLastReadPassage().getSequence();
        return new MyProgressResponse(sequence, clubMember.progressRate(), clubMember.getLastReadAt());
    }

    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.generate();
            if (!clubRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("참여 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private List<String> authorNames(Book book) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getName())
                .collect(Collectors.toList());
    }

    private Map<Long, List<String>> authorNamesByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getName(), Collectors.toList())));
    }
}
