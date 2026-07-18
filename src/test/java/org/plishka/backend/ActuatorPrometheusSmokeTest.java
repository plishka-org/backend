package org.plishka.backend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0"
)
@ActiveProfiles("test")
class ActuatorPrometheusSmokeTest {
    @LocalManagementPort
    private int managementPort;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void prometheusEndpoint_ShouldExposeMetricsOnManagementPort() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://localhost:%d/actuator/prometheus".formatted(managementPort)))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("jvm_memory_used_bytes"));
    }
}
