package yeobaek.backend.club.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.BookRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.JoinCodeGenerator;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.ClubCreateResponse;
import yeobaek.backend.club.dto.ClubDetailResponse;
import yeobaek.backend.club.dto.ClubJoinResponse;
import yeobaek.backend.club.dto.ClubMemberResponse;
import yeobaek.backend.club.dto.MyClubResponse;
import yeobaek.backend.club.dto.MyClubsResponse;
import yeobaek.backend.club.dto.MyProgressResponse;
import yeobaek.backend.club.repository.ClubMemberCount;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

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
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND));
        Club club = clubRepository.save(new Club(name, book, generateUniqueJoinCode()));
        clubMemberRepository.save(new ClubMember(memberRepository.getReferenceById(memberId), club));
        return new ClubCreateResponse(club.getId(), club.getName(), club.getJoinCode(),
                ClubBookResponse.of(book, authorNames(book)));
    }

    @Transactional
    public ClubJoinResponse join(Long memberId, String joinCode) {
        Club club = clubRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_CODE_NOT_FOUND));
        clubMemberRepository.findByMemberIdAndClubId(memberId, club.getId())
                .ifPresentOrElse(ClubMember::activate,
                        () -> clubMemberRepository.save(
                                new ClubMember(memberRepository.getReferenceById(memberId), club)));
        Book book = club.getBook();
        return new ClubJoinResponse(club.getId(), club.getName(), ClubBookResponse.of(book, authorNames(book)));
    }

    @Transactional
    public void leave(Long memberId, Long clubId) {
        clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        ClubMember clubMember = clubMemberRepository.findByMemberIdAndClubId(memberId, clubId)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));
        clubMember.disable();
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
                            toMyProgress(clubMember));
                })
                .toList());
    }

    @Transactional(readOnly = true)
    public ClubDetailResponse findDetail(Long memberId, Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND));
        List<ClubMember> clubMembers = clubMemberRepository.findAllWithMemberByClubId(clubId);
        ClubMember myMembership = clubMembers.stream()
                .filter(clubMember -> clubMember.isOwnedBy(memberId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER));
        Book book = club.getBook();
        return new ClubDetailResponse(club.getId(), club.getName(), club.getJoinCode(),
                ClubBookResponse.of(book, authorNames(book)),
                toMyProgress(myMembership),
                clubMembers.stream()
                        .map(clubMember -> new ClubMemberResponse(clubMember.getMember().getId(),
                                clubMember.getMember().getNickname(), clubMember.isOwnedBy(memberId)))
                        .toList());
    }

    private MyProgressResponse toMyProgress(ClubMember clubMember) {
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
