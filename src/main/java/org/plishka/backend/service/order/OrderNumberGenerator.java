package org.plishka.backend.service.order;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {
    public String generate() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
