package org.plishka.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.repository.user.RoleRepository;
import org.plishka.backend.service.notification.ResendEmailClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {
    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private ResendEmailClient resendEmailClient;

    @Test
    void contextLoads() {
        assertTrue(roleRepository.findByName(Role.RoleName.USER).isPresent());
        assertTrue(roleRepository.findByName(Role.RoleName.ADMIN).isPresent());
    }

}
