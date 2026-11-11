package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonitoringRunResponse(
        @Schema(description = "GitHub Actions 실행 ID")
        long id,
        @Schema(description = "실행 시도 번호. 재실행 시 증가")
        int attempt,
        @Schema(description = "측정 UUID. 배포 실행이면 null")
        String session,
        @Schema(description = "실행 이름")
        String title,
        @Schema(description = "GitHub 실행 상태. completed이면 종료")
        String status,
        @Schema(description = "실행 결과. 종료 전에는 null")
        String conclusion,
        @Schema(description = "이 워크플로가 실행된 커밋 SHA. 앱에서 관측한 SHA와는 별개")
        String sha,
        @Schema(description = "실행 생성 시각 (UTC)")
        String createdAt,
        @Schema(description = "GitHub 실행 상세 주소")
        String url,
        @Schema(description = "연결한 배포의 단계: waiting, building, deploying, running, completed. 그 외 목록은 status와 동일")
        String phase
) {
}
