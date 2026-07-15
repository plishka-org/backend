package org.plishka.backend.monitoring.sentry;

import io.sentry.Sentry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SentryMonitoringService {
    private static final String UNKNOWN = "unknown";

    public void captureException(Throwable exception, String area, String operation) {
        Sentry.captureException(exception, scope -> {
            scope.setUser(null);
            scope.clearBreadcrumbs();
            scope.setTag("area", safeTagValue(area));
            scope.setTag("operation", safeTagValue(operation));
            scope.setTag("exception_class", exception.getClass().getSimpleName());
        });
    }

    private String safeTagValue(String value) {
        if (!StringUtils.hasText(value)) {
            return UNKNOWN;
        }
        return value;
    }
}
