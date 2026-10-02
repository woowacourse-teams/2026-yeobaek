package yeobaek.backend.club.api;

public class JoinCodeAllocationFailure extends IllegalStateException {

    public JoinCodeAllocationFailure() {
        super("참여 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
