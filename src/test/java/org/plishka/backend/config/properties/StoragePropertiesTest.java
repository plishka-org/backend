package org.plishka.backend.config.properties;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoragePropertiesTest {
    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validate_ShouldAcceptSevenDayDownloadPresignTtl() {
        assertTrue(validator.validate(storageProperties(Duration.ofDays(7))).isEmpty());
    }

    @Test
    void validate_ShouldRejectDownloadPresignTtlLongerThanSevenDays() {
        boolean hasDownloadTtlViolation = validator.validate(storageProperties(Duration.ofDays(8))).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("s3.downloadPresignTtl"));

        assertTrue(hasDownloadTtlViolation);
    }

    @Test
    void validate_ShouldRejectNonPositiveDownloadPresignTtl() {
        assertFalse(validator.validate(storageProperties(Duration.ZERO)).isEmpty());
    }

    private static StorageProperties storageProperties(Duration downloadPresignTtl) {
        return new StorageProperties(
                new StorageProperties.S3(
                        "test-bucket",
                        "eu-central-1",
                        "test-access-key",
                        "test-secret-key",
                        null,
                        Duration.ofMinutes(15),
                        downloadPresignTtl
                ),
                new StorageProperties.MediaConstraints(DataSize.ofMegabytes(10)),
                new StorageProperties.MediaConstraints(DataSize.ofMegabytes(200)),
                new StorageProperties.Cleanup(false, null)
        );
    }
}
