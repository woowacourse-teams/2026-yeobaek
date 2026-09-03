package yeobaek.backend.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public MemberCreateResponse create(String nickname) {
        Member member = new Member(nickname);
        if (memberRepository.existsByNickname(member.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        Member savedMember = memberRepository.save(member);
        return new MemberCreateResponse(savedMember.getId(), savedMember.getNickname());
    }

    @Transactional
    public void delete(Long memberId) {
        commentRepository.deleteAllByMemberId(memberId);
        clubMemberRepository.deleteAllByMemberId(memberId);
        memberRepository.deleteById(memberId);
    }
}
