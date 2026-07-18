package org.plishka.backend.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.util.TokenHashUtil;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

@Component
public class RateLimitRuleResolver {
    public static final String EMAIL_FIELD = "email";
    public static final String TOKEN_FIELD = "token";

    private static final String DEVICE_ID_HEADER = "Device-Id";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public Set<String> requiredBodyFields(HttpServletRequest request) {
        String method = request.getMethod();
        String path = requestPath(request);

        if (isPost(method, path, "/auth/login")
                || isPost(method, path, "/auth/register")
                || isPost(method, path, "/auth/resend-verification")
                || isPost(method, path, "/auth/forgot-password")) {
            return Set.of(EMAIL_FIELD);
        }

        if (isPost(method, path, "/auth/reset-password")
                || isPost(method, path, "/auth/verify-email-change")) {
            return Set.of(TOKEN_FIELD);
        }

        return Set.of();
    }

    public List<RateLimitKey> resolve(HttpServletRequest request, Map<String, String> bodyFields) {
        RateLimitRequestContext context = requestContext(request, bodyFields);

        List<RateLimitKey> publicKeys = resolvePublicKeys(context);
        if (!publicKeys.isEmpty()) {
            return publicKeys;
        }

        Optional<CurrentUser> currentUser = currentUser();
        if (currentUser.isEmpty()) {
            return List.of();
        }

        if (isAdminPath(context.path()) && !currentUser.get().admin()) {
            return List.of();
        }

        return resolveAuthenticatedKeys(context, currentUser.get());
    }

    private RateLimitRequestContext requestContext(HttpServletRequest request, Map<String, String> bodyFields) {
        return new RateLimitRequestContext(
                request.getMethod(),
                requestPath(request),
                request.getRemoteAddr(),
                request,
                bodyFields
        );
    }

    private List<RateLimitKey> resolvePublicKeys(RateLimitRequestContext context) {
        List<RateLimitKey> authKeys = resolvePublicAuthKeys(context);
        if (!authKeys.isEmpty()) {
            return authKeys;
        }

        List<RateLimitKey> fileKeys = resolvePublicFileKeys(context);
        if (!fileKeys.isEmpty()) {
            return fileKeys;
        }

        return resolvePublicReadKeys(context);
    }

    private List<RateLimitKey> resolvePublicAuthKeys(RateLimitRequestContext context) {
        String method = context.method();
        String path = context.path();
        String ip = context.ip();
        HttpServletRequest request = context.request();
        Map<String, String> bodyFields = context.bodyFields();

        if (isPost(method, path, "/auth/login")) {
            List<RateLimitKey> keys = new ArrayList<>();
            normalizedEmail(bodyFields).ifPresent(email -> {
                keys.add(key(RateLimitPolicy.AUTH_LOGIN_EMAIL, email));
                keys.add(key(RateLimitPolicy.AUTH_LOGIN_EMAIL_IP, "%s:%s".formatted(email, ip)));
            });
            keys.add(key(RateLimitPolicy.AUTH_LOGIN_IP, ip));
            return keys;
        }

        if (isPost(method, path, "/auth/register")) {
            List<RateLimitKey> keys = new ArrayList<>();
            keys.add(key(RateLimitPolicy.AUTH_REGISTER_IP, ip));
            normalizedEmail(bodyFields).ifPresent(email -> keys.add(key(
                    RateLimitPolicy.AUTH_REGISTER_EMAIL,
                    email
            )));
            return keys;
        }

        if (isPost(method, path, "/auth/resend-verification")) {
            return emailAndIpKeys(
                    bodyFields,
                    ip,
                    RateLimitPolicy.AUTH_RESEND_EMAIL,
                    RateLimitPolicy.AUTH_RESEND_IP
            );
        }

        if (isPost(method, path, "/auth/forgot-password")) {
            return emailAndIpKeys(
                    bodyFields,
                    ip,
                    RateLimitPolicy.AUTH_FORGOT_EMAIL,
                    RateLimitPolicy.AUTH_FORGOT_IP
            );
        }

        if (isGet(method, path, "/auth/verify")) {
            List<RateLimitKey> keys = new ArrayList<>();
            keys.add(key(RateLimitPolicy.AUTH_VERIFY_IP, ip));
            tokenHash(request.getParameter(TOKEN_FIELD)).ifPresent(hash -> keys.add(key(
                    RateLimitPolicy.AUTH_VERIFY_TOKEN,
                    hash
            )));
            return keys;
        }

        if (isPost(method, path, "/auth/verify-email-change")) {
            return tokenAndIpKeys(
                    bodyFields.get(TOKEN_FIELD),
                    ip,
                    RateLimitPolicy.AUTH_EMAIL_CHANGE_VERIFY_IP,
                    RateLimitPolicy.AUTH_EMAIL_CHANGE_VERIFY_TOKEN
            );
        }

        if (isPost(method, path, "/auth/reset-password")) {
            return tokenAndIpKeys(
                    bodyFields.get(TOKEN_FIELD),
                    ip,
                    RateLimitPolicy.AUTH_RESET_IP,
                    RateLimitPolicy.AUTH_RESET_TOKEN
            );
        }

        if (isPost(method, path, "/auth/refresh")) {
            List<RateLimitKey> keys = new ArrayList<>();
            String deviceId = request.getHeader(DEVICE_ID_HEADER);
            if (StringUtils.hasText(deviceId)) {
                keys.add(key(
                        RateLimitPolicy.AUTH_REFRESH_DEVICE_IP,
                        "%s:%s".formatted(deviceId.trim(), ip)
                ));
            }
            keys.add(key(RateLimitPolicy.AUTH_REFRESH_IP, ip));
            return keys;
        }

        return List.of();
    }

    private List<RateLimitKey> resolvePublicFileKeys(RateLimitRequestContext context) {
        if (isPost(context.method(), context.path(), "/files/presign/download")) {
            return List.of(key(
                    RateLimitPolicy.FILE_DOWNLOAD_PRESIGN,
                    context.ip()
            ));
        }

        return List.of();
    }

    private List<RateLimitKey> resolvePublicReadKeys(RateLimitRequestContext context) {
        if (isPublicRead(context.method(), context.path())) {
            return List.of(key(RateLimitPolicy.PUBLIC_READ, context.ip()));
        }

        return List.of();
    }

    private List<RateLimitKey> resolveAuthenticatedKeys(RateLimitRequestContext context, CurrentUser currentUser) {
        String method = context.method();
        String path = context.path();
        Long userId = currentUser.userId();
        HttpServletRequest request = context.request();

        if (isPost(method, path, "/auth/logout")) {
            String deviceId = request.getHeader(DEVICE_ID_HEADER);
            if (!StringUtils.hasText(deviceId)) {
                return List.of();
            }
            return List.of(key(
                    RateLimitPolicy.AUTH_LOGOUT_DEVICE,
                    "%d:%s".formatted(userId, deviceId.trim())
            ));
        }

        if (isPut(method, path, "/users/me/email")) {
            return List.of(
                    key(RateLimitPolicy.ACCOUNT_SENSITIVE, userId.toString()),
                    key(RateLimitPolicy.ACCOUNT_EMAIL_CHANGE, userId.toString())
            );
        }

        if (isPut(method, path, "/users/me/password") || isDelete(method, path, "/users/me")) {
            return List.of(key(RateLimitPolicy.ACCOUNT_SENSITIVE, userId.toString()));
        }

        if (isPut(method, path, "/users/me")) {
            return List.of(key(RateLimitPolicy.PROFILE_UPDATE, userId.toString()));
        }

        if (isPost(method, path, "/callback")) {
            return List.of(key(RateLimitPolicy.CALLBACK_CREATE, userId.toString()));
        }

        if (isPost(method, path, "/orders")) {
            return List.of(key(RateLimitPolicy.ORDER_CREATE, userId.toString()));
        }

        if (isPost(method, path, "/users/me/orders/{orderId}/repeat")) {
            return List.of(key(RateLimitPolicy.ORDER_REPEAT, userId.toString()));
        }

        if (isCartWrite(method, path)) {
            return List.of(key(RateLimitPolicy.CART_WRITE, userId.toString()));
        }

        if (isFavoriteWrite(method, path)) {
            return List.of(key(RateLimitPolicy.FAVORITE_WRITE, userId.toString()));
        }

        if (isPost(method, path, "/products/{id}/view")) {
            return List.of(key(RateLimitPolicy.PRODUCT_VIEW, userId.toString()));
        }

        if (isAdminWrite(method, path)) {
            return List.of(key(RateLimitPolicy.ADMIN_WRITE, userId.toString()));
        }

        if (isAuthenticatedRead(method, path)) {
            return List.of(key(RateLimitPolicy.AUTHENTICATED_READ, userId.toString()));
        }

        return List.of();
    }

    private List<RateLimitKey> emailAndIpKeys(
            Map<String, String> bodyFields,
            String ip,
            RateLimitPolicy emailPolicy,
            RateLimitPolicy ipPolicy
    ) {
        List<RateLimitKey> keys = new ArrayList<>();
        normalizedEmail(bodyFields).ifPresent(email -> keys.add(key(emailPolicy, email)));
        keys.add(key(ipPolicy, ip));
        return keys;
    }

    private List<RateLimitKey> tokenAndIpKeys(
            String token,
            String ip,
            RateLimitPolicy ipPolicy,
            RateLimitPolicy tokenPolicy
    ) {
        List<RateLimitKey> keys = new ArrayList<>();
        keys.add(key(ipPolicy, ip));
        tokenHash(token).ifPresent(hash -> keys.add(key(tokenPolicy, hash)));
        return keys;
    }

    private boolean isCartWrite(String method, String path) {
        return isPost(method, path, "/cart/items")
                || isPut(method, path, "/cart/items/{productId}")
                || isDelete(method, path, "/cart/items/{productId}")
                || isDelete(method, path, "/cart")
                || isPost(method, path, "/cart/merge");
    }

    private boolean isFavoriteWrite(String method, String path) {
        return isPost(method, path, "/users/me/favorites/{productId}")
                || isDelete(method, path, "/users/me/favorites/{productId}");
    }

    private boolean isAdminWrite(String method, String path) {
        boolean writeMethod = HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
        return writeMethod && isAdminPath(path);
    }

    private boolean isAdminPath(String path) {
        return pathMatcher.match("/admin/**", path);
    }

    private boolean isPublicRead(String method, String path) {
        return HttpMethod.GET.matches(method)
                && (pathMatcher.match("/home", path)
                || pathMatcher.match("/about", path)
                || pathMatcher.match("/contacts-page", path)
                || pathMatcher.match("/categories", path)
                || pathMatcher.match("/products", path)
                || pathMatcher.match("/products/{id}", path)
                || pathMatcher.match("/products/{id}/related", path)
                || pathMatcher.match("/reviews", path)
                || pathMatcher.match("/reviews/{id}", path)
                || pathMatcher.match("/settings", path)
                || pathMatcher.match("/version", path));
    }

    private boolean isAuthenticatedRead(String method, String path) {
        return HttpMethod.GET.matches(method)
                && (pathMatcher.match("/cart", path)
                || pathMatcher.match("/users/me", path)
                || pathMatcher.match("/users/me/orders/**", path)
                || pathMatcher.match("/users/me/favorites", path)
                || pathMatcher.match("/users/me/viewed", path)
                || pathMatcher.match("/users/me/callback-requests", path)
                || pathMatcher.match("/admin/**", path));
    }

    private Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return Optional.empty();
        }

        Long userId = principal.getUserId();
        if (userId == null) {
            return Optional.empty();
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
        return Optional.of(new CurrentUser(userId, admin));
    }

    private Optional<String> normalizedEmail(Map<String, String> bodyFields) {
        String email = bodyFields.get(EMAIL_FIELD);
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }

        return Optional.of(UserInputNormalizer.normalizeEmail(email));
    }

    private Optional<String> tokenHash(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        return Optional.of(TokenHashUtil.sha256(token));
    }

    private RateLimitKey key(RateLimitPolicy policy, String identity) {
        return new RateLimitKey(policy, identity);
    }

    private boolean isGet(String method, String path, String pattern) {
        return HttpMethod.GET.matches(method) && pathMatcher.match(pattern, path);
    }

    private boolean isPost(String method, String path, String pattern) {
        return HttpMethod.POST.matches(method) && pathMatcher.match(pattern, path);
    }

    private boolean isPut(String method, String path, String pattern) {
        return HttpMethod.PUT.matches(method) && pathMatcher.match(pattern, path);
    }

    private boolean isDelete(String method, String path, String pattern) {
        return HttpMethod.DELETE.matches(method) && pathMatcher.match(pattern, path);
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private record CurrentUser(Long userId, boolean admin) {
    }

    private record RateLimitRequestContext(
            String method,
            String path,
            String ip,
            HttpServletRequest request,
            Map<String, String> bodyFields
    ) {
    }
}
