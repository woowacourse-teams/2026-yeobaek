package yeobaek.backend.club.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.club.api.ClubMembershipApi;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.foundation.identity.MemberId;

@Service
@RequiredArgsConstructor
public class ClubMembershipService implements ClubMembershipApi {

    private final ClubMemberRepository repository;

    @Override
    public void erase(MemberId memberId) {
        repository.deleteAllByMemberId(memberId.value());
    }
}
