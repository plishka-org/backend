package org.plishka.backend.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class VersionUtil {
    @Value("${spring.application.project_version}")
    private Integer version;
}
