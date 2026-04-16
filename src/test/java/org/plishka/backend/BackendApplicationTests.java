package org.plishka.backend;

import org.junit.jupiter.api.Test;
import org.plishka.backend.service.notification.ResendEmailClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @MockitoBean
    private ResendEmailClient resendEmailClient;

    @Test
    void contextLoads() {
    }

}
