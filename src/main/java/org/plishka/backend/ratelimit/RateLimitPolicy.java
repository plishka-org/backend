package org.plishka.backend.ratelimit;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * Known rate-limit policies.
 *
 * <p>{@code propertyName} connects this enum with {@code rate-limit-policies.yaml}. For example,
 * the enum constant {@code AUTH_LOGIN_IP} is configured by the YAML key {@code auth-login-ip}.
 * Application code should normally work with enum constants; {@code propertyName} is only needed
 * when binding external configuration to those constants.
 *
 * <p>Configured YAML values are the normal source of limits. {@code defaultBandwidths} are used
 * only if a known policy is missing from YAML, so the application still has a safe fallback.
 *
 * <p>A misspelled YAML policy key is ignored. The correctly named enum policy will then use its
 * fallback limits.
 */
@Getter
public enum RateLimitPolicy {
    AUTH_LOGIN_EMAIL("auth-login-email", bandwidths(
            bandwidth(20, Duration.ofHours(1)),
            bandwidth(100, Duration.ofDays(1))
    )),
    AUTH_LOGIN_EMAIL_IP("auth-login-email-ip", bandwidths(bandwidth(5, Duration.ofMinutes(10)))),
    AUTH_LOGIN_IP("auth-login-ip", bandwidths(bandwidth(60, Duration.ofMinutes(10)))),
    AUTH_REGISTER_IP("auth-register-ip", bandwidths(
            bandwidth(10, Duration.ofHours(1)),
            bandwidth(50, Duration.ofDays(1))
    )),
    AUTH_REGISTER_EMAIL("auth-register-email", bandwidths(bandwidth(3, Duration.ofDays(1)))),
    AUTH_RESEND_EMAIL("auth-resend-email", bandwidths(
            bandwidth(1, Duration.ofMinutes(2)),
            bandwidth(5, Duration.ofDays(1))
    )),
    AUTH_RESEND_IP("auth-resend-ip", bandwidths(
            bandwidth(20, Duration.ofHours(1)),
            bandwidth(100, Duration.ofDays(1))
    )),
    AUTH_FORGOT_EMAIL("auth-forgot-email", bandwidths(
            bandwidth(1, Duration.ofMinutes(2)),
            bandwidth(5, Duration.ofDays(1))
    )),
    AUTH_FORGOT_IP("auth-forgot-ip", bandwidths(
            bandwidth(20, Duration.ofHours(1)),
            bandwidth(100, Duration.ofDays(1))
    )),
    AUTH_VERIFY_IP("auth-verify-ip", bandwidths(bandwidth(60, Duration.ofHours(1)))),
    AUTH_VERIFY_TOKEN("auth-verify-token", bandwidths(bandwidth(10, Duration.ofHours(1)))),
    AUTH_EMAIL_CHANGE_VERIFY_IP("auth-email-change-verify-ip", bandwidths(bandwidth(30, Duration.ofHours(1)))),
    AUTH_EMAIL_CHANGE_VERIFY_TOKEN("auth-email-change-verify-token", bandwidths(
            bandwidth(10, Duration.ofHours(1))
    )),
    AUTH_RESET_IP("auth-reset-ip", bandwidths(bandwidth(30, Duration.ofHours(1)))),
    AUTH_RESET_TOKEN("auth-reset-token", bandwidths(bandwidth(10, Duration.ofHours(1)))),
    AUTH_REFRESH_DEVICE_IP("auth-refresh-device-ip", bandwidths(bandwidth(120, Duration.ofHours(1)))),
    AUTH_REFRESH_IP("auth-refresh-ip", bandwidths(bandwidth(600, Duration.ofHours(1)))),
    AUTH_LOGOUT_DEVICE("auth-logout-device", bandwidths(bandwidth(30, Duration.ofHours(1)))),
    ACCOUNT_SENSITIVE("account-sensitive", bandwidths(
            bandwidth(5, Duration.ofMinutes(15)),
            bandwidth(20, Duration.ofDays(1))
    )),
    ACCOUNT_EMAIL_CHANGE("account-email-change", bandwidths(
            bandwidth(1, Duration.ofMinutes(5)),
            bandwidth(5, Duration.ofDays(1))
    )),
    PROFILE_UPDATE("profile-update", bandwidths(bandwidth(30, Duration.ofHours(1)))),
    CALLBACK_CREATE("callback-create", bandwidths(
            bandwidth(1, Duration.ofMinutes(10)),
            bandwidth(5, Duration.ofDays(1))
    )),
    ORDER_CREATE("order-create", bandwidths(
            bandwidth(10, Duration.ofHours(1)),
            bandwidth(30, Duration.ofDays(1))
    )),
    ORDER_REPEAT("order-repeat", bandwidths(
            bandwidth(10, Duration.ofHours(1)),
            bandwidth(30, Duration.ofDays(1))
    )),
    CART_WRITE("cart-write", bandwidths(bandwidth(120, Duration.ofMinutes(1)))),
    FAVORITE_WRITE("favorite-write", bandwidths(bandwidth(120, Duration.ofMinutes(1)))),
    PRODUCT_VIEW("product-view", bandwidths(bandwidth(60, Duration.ofMinutes(1)))),
    FILE_DOWNLOAD_PRESIGN("file-download-presign", bandwidths(bandwidth(300, Duration.ofMinutes(1)))),
    ADMIN_WRITE("admin-write", bandwidths(bandwidth(120, Duration.ofMinutes(1)))),
    PUBLIC_READ("public-read", bandwidths(bandwidth(600, Duration.ofMinutes(1)))),
    AUTHENTICATED_READ("authenticated-read", bandwidths(bandwidth(600, Duration.ofMinutes(1))));

    private final String propertyName;
    private final List<BandwidthLimit> defaultBandwidths;

    static {
        verifyUniquePropertyNames();
    }

    RateLimitPolicy(String propertyName, List<BandwidthLimit> defaultBandwidths) {
        this.propertyName = propertyName;
        this.defaultBandwidths = defaultBandwidths;
    }

    private static void verifyUniquePropertyNames() {
        Set<String> propertyNames = new HashSet<>();
        for (RateLimitPolicy policy : values()) {
            if (!propertyNames.add(policy.propertyName)) {
                throw new IllegalStateException("Duplicate rate-limit propertyName: " + policy.propertyName);
            }
        }
    }

    private static BandwidthLimit bandwidth(long capacity, Duration period) {
        return new BandwidthLimit(capacity, period);
    }

    private static List<BandwidthLimit> bandwidths(BandwidthLimit... bandwidths) {
        return List.of(bandwidths);
    }

    public record BandwidthLimit(long capacity, Duration period) {
    }
}
