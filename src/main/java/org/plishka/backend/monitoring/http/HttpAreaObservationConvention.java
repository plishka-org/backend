package org.plishka.backend.monitoring.http;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.StringUtils;

public class HttpAreaObservationConvention extends DefaultServerRequestObservationConvention {
    private static final String AREA = "area";
    private static final String AREA_ADMIN = "admin";
    private static final String AREA_AUTH = "auth";
    private static final String AREA_CALLBACK = "callback";
    private static final String AREA_CART = "cart";
    private static final String AREA_FILE = "file";
    private static final String AREA_ORDER = "order";
    private static final String AREA_PUBLIC = "public";
    private static final String AREA_SETTINGS = "settings";
    private static final String AREA_UNKNOWN = "unknown";
    private static final String AREA_USER = "user";

    @Override
    @NonNull
    public KeyValues getLowCardinalityKeyValues(@NonNull ServerRequestObservationContext context) {
        return KeyValues.of(
                area(context),
                exception(context),
                method(context),
                outcome(context),
                status(context),
                uri(context)
        );
    }

    private KeyValue area(ServerRequestObservationContext context) {
        return KeyValue.of(AREA, resolveArea(context));
    }

    private String resolveArea(ServerRequestObservationContext context) {
        String path = context.getPathPattern();
        HttpServletRequest request = context.getCarrier();
        if (!StringUtils.hasText(path) && request != null) {
            path = requestPath(request);
        }

        if (!StringUtils.hasText(path)) {
            return AREA_UNKNOWN;
        }

        if (path.startsWith("/admin")) {
            return AREA_ADMIN;
        }
        if (path.startsWith("/auth")) {
            return AREA_AUTH;
        }
        if (path.startsWith("/cart")) {
            return AREA_CART;
        }
        if (path.startsWith("/orders") || path.startsWith("/users/me/orders")) {
            return AREA_ORDER;
        }
        if (path.startsWith("/files")) {
            return AREA_FILE;
        }
        if (path.startsWith("/users")) {
            return AREA_USER;
        }
        if (path.startsWith("/callback")) {
            return AREA_CALLBACK;
        }
        if (path.startsWith("/settings")) {
            return AREA_SETTINGS;
        }
        if (isPublicArea(path)) {
            return AREA_PUBLIC;
        }
        return AREA_UNKNOWN;
    }

    private boolean isPublicArea(String path) {
        return path.startsWith("/home")
                || path.startsWith("/products")
                || path.startsWith("/categories")
                || path.startsWith("/reviews")
                || path.startsWith("/about")
                || path.startsWith("/contacts-page")
                || path.startsWith("/version")
                || path.startsWith("/health");
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
