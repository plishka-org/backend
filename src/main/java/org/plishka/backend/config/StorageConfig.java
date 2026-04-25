package org.plishka.backend.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {
    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(region())
                .credentialsProvider(credentialsProvider());

        if (hasCustomEndpoint()) {
            builder.endpointOverride(endpointUri())
                    .serviceConfiguration(s3Configuration());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(region())
                .credentialsProvider(credentialsProvider());

        if (hasCustomEndpoint()) {
            builder.endpointOverride(endpointUri())
                    .serviceConfiguration(s3Configuration());
        }

        return builder.build();
    }

    private Region region() {
        return Region.of(storageProperties.s3().region());
    }

    private StaticCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                storageProperties.s3().accessKey(),
                storageProperties.s3().secretKey()
        );

        return StaticCredentialsProvider.create(credentials);
    }

    private boolean hasCustomEndpoint() {
        return StringUtils.hasText(storageProperties.s3().endpointUrl());
    }

    private URI endpointUri() {
        return URI.create(storageProperties.s3().endpointUrl());
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }
}
