package org.plishka.backend.service.notification.email.transport.resend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.ResendProperties;
import org.plishka.backend.exception.InvalidEmailRecipientException;
import org.plishka.backend.exception.NonRetryableEmailException;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.service.notification.email.EmailType;
import org.plishka.backend.service.notification.email.transport.EmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class ResendEmailTransport implements EmailTransport {
    private static final URI RESEND_EMAILS_URI = URI.create("https://api.resend.com/emails");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final URI emailsUri;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String fromEmail;

    @Autowired
    public ResendEmailTransport(ObjectMapper objectMapper, ResendProperties resendProperties) {
        this(objectMapper, resendProperties, defaultHttpClient(), RESEND_EMAILS_URI);
    }

    ResendEmailTransport(
            ObjectMapper objectMapper,
            ResendProperties resendProperties,
            HttpClient httpClient,
            URI emailsUri
    ) {
        this.httpClient = httpClient;
        this.emailsUri = emailsUri;
        this.objectMapper = objectMapper;
        this.apiKey = resendProperties.apiKey();
        this.fromEmail = resendProperties.fromEmail();
    }

    @Override
    public void sendEmail(EmailType emailType, String to, String subject, String text) {
        if (!StringUtils.hasText(to)) {
            throw new InvalidEmailRecipientException("Email recipient must not be blank");
        }

        try {
            String requestBodyJson = buildRequestBodyJson(to, subject, text);
            HttpRequest request = buildHttpRequest(requestBodyJson);

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            throwIfRequestFailed(response, emailType);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NonRetryableEmailException("Email sending was interrupted");
        } catch (IOException exception) {
            throw new RetryableEmailException("Failed to call Resend API", exception);
        }
    }

    private void throwIfRequestFailed(HttpResponse<String> response, EmailType emailType) {
        int status = response.statusCode();
        if (isSuccessful(response)) {
            return;
        }

        log.warn(
                "Resend email request failed: status={}, emailType={}",
                status,
                emailType.getMetricValue()
        );

        if (status == 429 || status >= 500) {
            throw new RetryableEmailException("Resend temporary failure with status " + status);
        }

        throw new NonRetryableEmailException("Resend permanent failure with status " + status);
    }

    private boolean isSuccessful(HttpResponse<String> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private HttpRequest buildHttpRequest(String requestBodyJson) {
        return HttpRequest.newBuilder(emailsUri)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
    }

    private String buildRequestBodyJson(String to, String subject, String text) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("from", fromEmail);
        requestBody.put("to", List.of(to));
        requestBody.put("subject", subject);
        requestBody.put("text", text);

        return objectMapper.writeValueAsString(requestBody);
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }
}
