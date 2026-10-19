package yeobaek.backend.notification.domain;

import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record FcmSendFailure(
        String fid,
        String errorCode,
        String sdkErrorCode,
        Integer httpStatus,
        String retryAfter
) {

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String RECEIPT_UNCERTAIN = "RECEIPT_UNCERTAIN";
    private static final String UNREGISTERED = "UNREGISTERED";
    private static final String UNAVAILABLE = "UNAVAILABLE";
    private static final int MAX_RETRIES = 5;
    private static final int MAX_JITTER_PERCENT = 20;
    private static final Duration VALIDITY = Duration.ofMinutes(30);
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(4),
            Duration.ofMinutes(5), Duration.ofMinutes(5));

    public static FcmSendFailure from(String fid, FirebaseMessagingException exception) {
        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        IncomingHttpResponse httpResponse = exception.getHttpResponse();
        return new FcmSendFailure(
                fid,
                errorCode == null ? UNKNOWN : errorCode.name(),
                exception.getErrorCode().name(),
                httpResponse == null ? null : httpResponse.getStatusCode(),
                httpResponse == null ? null : retryAfter(httpResponse));
    }

    public boolean isRetryable() {
        return switch (handlingCode()) {
            case UNAVAILABLE, "INTERNAL", "QUOTA_EXCEEDED", RECEIPT_UNCERTAIN -> true;
            default -> false;
        };
    }

    public boolean isUnregistered() {
        return UNREGISTERED.equals(handlingCode());
    }

    public boolean isReceiptUncertain() {
        return RECEIPT_UNCERTAIN.equals(handlingCode());
    }

    public String handlingCode() {
        Integer detailStatus = statusForCode(errorCode);
        Integer sdkStatus = statusForCode(sdkErrorCode);
        if (!UNKNOWN.equals(sdkErrorCode) && sdkStatus == null
                && (!UNKNOWN.equals(errorCode) || httpStatus != null)) {
            return UNKNOWN;
        }
        if (conflicts(detailStatus, sdkStatus) || conflicts(detailStatus, httpStatus)
                || conflicts(sdkStatus, httpStatus)) {
            return UNKNOWN;
        }
        if (!UNKNOWN.equals(errorCode)) {
            return detailStatus == null ? UNKNOWN : errorCode;
        }
        if (httpStatus != null) {
            return switch (httpStatus) {
                case 400 -> "INVALID_ARGUMENT";
                case 401 -> "UNAUTHENTICATED";
                case 403 -> "PERMISSION_DENIED";
                case 404 -> "NOT_FOUND";
                case 429 -> "QUOTA_EXCEEDED";
                case 500 -> "INTERNAL";
                case 503 -> UNAVAILABLE;
                default -> UNKNOWN;
            };
        }
        return switch (sdkErrorCode) {
            case "INVALID_ARGUMENT", "UNAUTHENTICATED", "PERMISSION_DENIED", "NOT_FOUND" -> sdkErrorCode;
            case "DEADLINE_EXCEEDED", UNAVAILABLE -> RECEIPT_UNCERTAIN;
            default -> UNKNOWN;
        };
    }

    public boolean hasInvalidRetryAfter() {
        return retryAfter != null && !retryAfter.isBlank() && !retryAfter.trim().matches("[0-9]+")
                && retryAfterDate().isEmpty();
    }

    // 재시도를 예약하거나 실행하지 않고, 주어진 이력으로 다음 시도 시각만 계산한다.
    public Optional<Instant> nextRetryAt(Instant commentCreatedAt, Instant observedAt,
                                        int retriesPerformed, boolean uncertainRetryUsed, int jitterPercent) {
        if (retriesPerformed < 0 || jitterPercent < 0 || jitterPercent > MAX_JITTER_PERCENT
                || observedAt.isBefore(commentCreatedAt)) {
            throw new IllegalArgumentException("재시도 이력과 시각, 지연 비율을 확인하세요.");
        }
        Instant expiresAt = commentCreatedAt.plus(VALIDITY);
        if (!isRetryable() || retriesPerformed >= MAX_RETRIES || !observedAt.isBefore(expiresAt)
                || (isReceiptUncertain() && uncertainRetryUsed)) {
            return Optional.empty();
        }
        Duration delay = RETRY_DELAYS.get(retriesPerformed);
        Instant backoffAt = observedAt.plus(delay);
        Instant serverRetryAt = retryAfterAt(observedAt);
        try {
            Instant next = (serverRetryAt.isAfter(backoffAt) ? serverRetryAt : backoffAt)
                    .plusMillis(delay.toMillis() * jitterPercent / 100);
            return next.isBefore(expiresAt) ? Optional.of(next) : Optional.empty();
        } catch (DateTimeException exception) {
            return Optional.empty();
        }
    }

    private Instant retryAfterAt(Instant observedAt) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return observedAt;
        }
        String value = retryAfter.trim();
        if (value.matches("[0-9]+")) {
            try {
                return observedAt.plusSeconds(Long.parseLong(value));
            } catch (NumberFormatException | DateTimeException | ArithmeticException exception) {
                // 유효한 숫자지만 표현 범위를 넘는 긴 대기 요구를 짧은 대기로 대체하지 않는다.
                return Instant.MAX;
            }
        }
        return retryAfterDate().orElse(observedAt);
    }

    private Optional<Instant> retryAfterDate() {
        try {
            return Optional.of(ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private boolean conflicts(Integer first, Integer second) {
        return first != null && second != null && !first.equals(second);
    }

    private Integer statusForCode(String code) {
        return switch (code) {
            case "INVALID_ARGUMENT" -> 400;
            case "UNAUTHENTICATED", "THIRD_PARTY_AUTH_ERROR" -> 401;
            case "PERMISSION_DENIED", "SENDER_ID_MISMATCH" -> 403;
            case "NOT_FOUND", UNREGISTERED -> 404;
            case "RESOURCE_EXHAUSTED", "QUOTA_EXCEEDED" -> 429;
            case "INTERNAL" -> 500;
            case UNAVAILABLE -> 503;
            default -> null;
        };
    }

    private static String retryAfter(IncomingHttpResponse response) {
        for (Map.Entry<String, Object> header : response.getHeaders().entrySet()) {
            if (RETRY_AFTER_HEADER.equalsIgnoreCase(header.getKey())) {
                Object value = header.getValue();
                if (value instanceof List<?> values) {
                    return values.isEmpty() ? null : values.getFirst().toString();
                }
                return value.toString();
            }
        }
        return null;
    }
}
