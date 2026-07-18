package org.plishka.backend.controller.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class PublicHealthController {
    private static final Map<String, String> HEALTH_RESPONSE = Map.of("status", "UP");

    @GetMapping
    public Map<String, String> health() {
        return HEALTH_RESPONSE;
    }
}
