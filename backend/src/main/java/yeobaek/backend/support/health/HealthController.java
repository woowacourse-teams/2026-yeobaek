package yeobaek.backend.support.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final HealthCheckService healthCheckService;
    private final String deployedCommitSha;

    public HealthController(
            HealthCheckService healthCheckService,
            @Value("${deployment.commit-sha}") String deployedCommitSha
    ) {
        this.healthCheckService = healthCheckService;
        this.deployedCommitSha = deployedCommitSha;
    }

    @GetMapping("/health")
    public ResponseEntity<Void> checkHealth() {
        try {
            healthCheckService.checkDatabase();
            return ResponseEntity.ok()
                    .header("Deployed-Commit-SHA", deployedCommitSha)
                    .build();
        } catch (DataAccessException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
