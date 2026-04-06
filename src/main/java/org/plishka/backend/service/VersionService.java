package org.plishka.backend.service;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.util.VersionUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VersionService {
    private final VersionUtil versionUtil;

    public Integer getCurrentVersion() {
        return versionUtil.getVersion();
    }
}
