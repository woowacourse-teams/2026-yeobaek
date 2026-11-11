package yeobaek.backend.admin.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DeploymentObservation {

    private static final Duration OBSERVATION_DURATION = Duration.ofMinutes(5);

    private final List<DeploymentProgress> deployments;

    public DeploymentObservation(List<DeploymentProgress> deployments) {
        this.deployments = List.copyOf(deployments);
    }

    public Optional<Instant> endsAt() {
        List<Optional<Instant>> endings = deployments.stream().map(DeploymentProgress::completedAt).toList();
        if (endings.isEmpty() || endings.stream().anyMatch(Optional::isEmpty)) {
            return Optional.empty();
        }

        return endings.stream()
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .map(end -> end.plus(OBSERVATION_DURATION));
    }
}
