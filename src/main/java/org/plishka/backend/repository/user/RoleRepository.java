package org.plishka.backend.repository.user;

import java.util.Optional;
import org.plishka.backend.domain.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(Role.RoleName name);
}
