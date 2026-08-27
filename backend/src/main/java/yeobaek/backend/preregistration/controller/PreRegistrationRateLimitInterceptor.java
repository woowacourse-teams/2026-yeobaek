package yeobaek.backend.preregistration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
class PreRegistrationRateLimitInterceptor implements HandlerInterceptor {

    private static final String REAL_IP_HEADER = "X-Real-IP";
    private static final int MAX_IP_LENGTH = 45;
    private static final int IPV4_OCTET_COUNT = 4;
    private static final int MAX_COMPRESSED_IPV6_SECTIONS = 2;

    private final PreRegistrationRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        rateLimiter.check(resolveClientIp(request));
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String realIp = request.getHeader(REAL_IP_HEADER);
        if (realIp != null) {
            String candidate = realIp.trim();
            if (isValidIpAddress(candidate)) {
                return candidate;
            }
        }
        return remoteAddress;
    }

    private boolean isValidIpAddress(String address) {
        if (address.isEmpty() || address.length() > MAX_IP_LENGTH) {
            return false;
        }
        return address.indexOf(':') >= 0 ? isValidIpv6(address) : isValidIpv4(address);
    }

    private boolean isValidIpv4(String address) {
        String[] octets = address.split("\\.", -1);
        if (octets.length != IPV4_OCTET_COUNT) {
            return false;
        }
        for (String octet : octets) {
            if (!isValidIpv4Octet(octet)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidIpv4Octet(String octet) {
        if (octet.isEmpty() || octet.length() > 3) {
            return false;
        }
        int value = 0;
        for (int index = 0; index < octet.length(); index++) {
            char character = octet.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
            value = value * 10 + character - '0';
        }
        return value <= 255;
    }

    private boolean isValidIpv6(String address) {
        String[] compressedSections = address.split("::", -1);
        if (compressedSections.length > MAX_COMPRESSED_IPV6_SECTIONS) {
            return false;
        }

        boolean compressed = compressedSections.length == 2;
        int leftGroups = countIpv6Groups(compressedSections[0], !compressed);
        int rightGroups = compressed ? countIpv6Groups(compressedSections[1], true) : 0;
        if (leftGroups < 0 || rightGroups < 0) {
            return false;
        }
        int groupCount = leftGroups + rightGroups;
        return compressed ? groupCount < 8 : groupCount == 8;
    }

    private int countIpv6Groups(String section, boolean allowIpv4AtEnd) {
        if (section.isEmpty()) {
            return 0;
        }

        String[] groups = section.split(":", -1);
        int groupCount = 0;
        for (int index = 0; index < groups.length; index++) {
            String group = groups[index];
            if (group.indexOf('.') >= 0) {
                if (!allowIpv4AtEnd || index != groups.length - 1 || !isValidIpv4(group)) {
                    return -1;
                }
                groupCount += 2;
            } else if (!isValidIpv6Group(group)) {
                return -1;
            } else {
                groupCount++;
            }
        }
        return groupCount;
    }

    private boolean isValidIpv6Group(String group) {
        if (group.isEmpty() || group.length() > 4) {
            return false;
        }
        for (int index = 0; index < group.length(); index++) {
            char character = group.charAt(index);
            boolean hexadecimal = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F');
            if (!hexadecimal) {
                return false;
            }
        }
        return true;
    }
}
