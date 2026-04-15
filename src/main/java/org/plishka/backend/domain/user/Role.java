package org.plishka.backend.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role implements GrantedAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    @Override
    public String getAuthority() {
        return "ROLE_" + name.name();
    }

    public enum RoleName {
        USER,
        ADMIN
    }
}
