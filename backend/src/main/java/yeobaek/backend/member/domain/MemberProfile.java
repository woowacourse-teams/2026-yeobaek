package yeobaek.backend.member.domain;

import yeobaek.backend.shared.identity.MemberId;

public record MemberProfile(MemberId id, String nickname) {

    public MemberProfile {
        if (id == null) {
            throw new IllegalArgumentException("회원 식별자는 필수입니다.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("회원 닉네임은 필수입니다.");
        }
    }
}
