package org.plishka.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.service.VersionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/version")
@Tag(name = "Version")
public class VersionController {
    private final VersionService versionService;

    @Operation(
            operationId = "getVersion",
            summary = "Get API version",
            description = "Returns the current backend version exposed by the existing version service."
    )
    @GetMapping
    public Integer getCurrentVersion() {
        return versionService.getCurrentVersion();
    }
}
