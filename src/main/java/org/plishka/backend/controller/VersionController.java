package org.plishka.backend.controller;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.service.VersionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/version")
public class VersionController {
    private final VersionService versionService;

    @GetMapping
    public Integer getCurrentVersion() {
        return versionService.getCurrentVersion();
    }
}
