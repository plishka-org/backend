package org.plishka.backend.service.notification;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.service.notification.email.EmailType;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;
import org.plishka.backend.service.notification.email.transport.RetryableEmailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

@SpringJUnitConfig(classes = {
        AsyncEmailSender.class,
        AsyncEmailSenderSpringTest.AsyncTestConfig.class
})
class AsyncEmailSenderSpringTest {
    private static final String USER_EMAIL = "user@example.com";
    private static final String SUBJECT = "Subject";
    private static final String TEXT = "Body";

    @MockitoBean
    private RetryableEmailSender retryableEmailSender;

    @Autowired
    private AsyncEmailSender asyncEmailSender;

    @Test
    void sendEmailAsync_ShouldDispatchThroughConfiguredExecutor() throws Exception {
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            delivered.countDown();
            return null;
        }).when(retryableEmailSender).sendEmail(EmailType.VERIFICATION, USER_EMAIL, SUBJECT, TEXT);

        asyncEmailSender.sendEmailAsync(EmailType.VERIFICATION, USER_EMAIL, SUBJECT, TEXT);

        assertTrue(delivered.await(2, TimeUnit.SECONDS));
        assertNotNull(threadName.get());
        assertTrue(threadName.get().startsWith("email-test-"));
    }

    @Configuration
    @EnableAsync
    static class AsyncTestConfig {
        @Bean(name = "emailTaskExecutor", destroyMethod = "shutdown")
        ThreadPoolTaskExecutor emailTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setThreadNamePrefix("email-test-");
            executor.initialize();
            return executor;
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        EmailMetricsRecorder emailMetricsRecorder(MeterRegistry meterRegistry) {
            return new EmailMetricsRecorder(meterRegistry);
        }

        @Bean
        SentryMonitoringService sentryMonitoringService() {
            return new SentryMonitoringService();
        }
    }
}
