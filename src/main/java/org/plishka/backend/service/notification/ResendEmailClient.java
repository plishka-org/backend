package org.plishka.backend.service.notification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.exception.ResendEmailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class ResendEmailClient {
    private static final URI RESEND_EMAILS_URI = URI.create("https://api.resend.com/emails");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String fromEmail;

    public ResendEmailClient(
            ObjectMapper objectMapper,
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            String requestBodyJson = buildRequestBodyJson(to, subject, text);
            HttpRequest request = buildHttpRequest(requestBodyJson);

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            throwIfRequestFailed(response, to, subject);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResendEmailException("Email sending was interrupted", exception);
        } catch (IOException exception) {
            throw new ResendEmailException("Failed to call Resend API", exception);
        }
    }

    private void throwIfRequestFailed(HttpResponse<String> response, String to, String subject) {
        if (isSuccessful(response)) {
            return;
        }

        log.warn(
                "Resend email request failed: status={}, to={}, subject={}",
                response.statusCode(),
                to,
                subject
        );

        throw new ResendEmailException("Resend email request failed with status " + response.statusCode());
    }

    private boolean isSuccessful(HttpResponse<String> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private HttpRequest buildHttpRequest(String requestBodyJson) {
        return HttpRequest.newBuilder(RESEND_EMAILS_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
    }

    private String buildRequestBodyJson(String to, String subject, String text) {
        SendEmailRequest requestBody = new SendEmailRequest(
                fromEmail,
                List.of(to),
                subject,
                text
        );

        return objectMapper.writeValueAsString(requestBody);
    }

    private record SendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String text
    ) {
    }
}
