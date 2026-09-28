package org.plishka.backend.service.notification.email.transport.resend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.config.properties.ResendProperties;
import org.plishka.backend.exception.InvalidEmailRecipientException;
import org.plishka.backend.exception.NonRetryableEmailException;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.service.notification.email.EmailType;
import tools.jackson.databind.ObjectMapper;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResendEmailTransportTest {
    private static final String SUBJECT = "Subject";
    private static final String TO_EMAIL = "user@example.com";
    private static final String BODY = "text";
    private static final String EXPECTED_REQUEST_BODY = "{\"from\":\"Plishka <noreply@example.com>\","
            + "\"to\":[\"user@example.com\"],\"subject\":\"Subject\",\"text\":\"text\"}";

    private HttpServer httpServer;
    private ResendEmailTransport resendEmailTransport;
    private int responseStatus;
    private String capturedAuthorizationHeader;
    private String capturedRequestBody;

    @BeforeEach
    void setUp() throws IOException {
        InetAddress loopbackAddress = InetAddress.getByName("127.0.0.1");
        httpServer = HttpServer.create(new InetSocketAddress(loopbackAddress, 0), 0);
        httpServer.createContext("/emails", this::handleEmailRequest);
        httpServer.start();

        resendEmailTransport = new ResendEmailTransport(
                new ObjectMapper(),
                new ResendProperties("test-api-key", "Plishka <noreply@example.com>"),
                HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1:%d/emails".formatted(httpServer.getAddress().getPort()))
        );
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void sendEmail_ShouldSendJsonRequest_WhenResendReturnsSuccess() throws JSONException {
        responseStatus = 202;

        assertDoesNotThrow(() -> sendEmail());

        assertEquals("Bearer test-api-key", capturedAuthorizationHeader);
        assertEquals(EXPECTED_REQUEST_BODY, capturedRequestBody, true);
    }

    @Test
    void sendEmail_ShouldThrowRetryableException_WhenResendReturnsRateLimit() {
        responseStatus = 429;

        assertThrows(RetryableEmailException.class, this::sendEmail);
    }

    @Test
    void sendEmail_ShouldThrowRetryableException_WhenResendReturnsServerError() {
        responseStatus = 503;

        assertThrows(RetryableEmailException.class, this::sendEmail);
    }

    @Test
    void sendEmail_ShouldThrowNonRetryableException_WhenResendReturnsClientError() {
        responseStatus = 400;

        assertThrows(NonRetryableEmailException.class, this::sendEmail);
    }

    @Test
    void sendEmail_ShouldRejectBlankRecipient() {
        InvalidEmailRecipientException exception = assertThrows(
                InvalidEmailRecipientException.class,
                () -> resendEmailTransport.sendEmail(EmailType.VERIFICATION, "  ", SUBJECT, BODY)
        );

        assertEquals("Email recipient must not be blank", exception.getMessage());
    }

    @Test
    void sendEmail_ShouldRejectNullRecipient() {
        assertThrows(
                InvalidEmailRecipientException.class,
                () -> resendEmailTransport.sendEmail(EmailType.VERIFICATION, null, SUBJECT, BODY)
        );
    }

    private void sendEmail() {
        resendEmailTransport.sendEmail(EmailType.VERIFICATION, TO_EMAIL, SUBJECT, BODY);
    }

    private void handleEmailRequest(HttpExchange exchange) throws IOException {
        capturedAuthorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
        capturedRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseStatus, response.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(response);
        }
    }
}
