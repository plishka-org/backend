package org.plishka.backend.domain.contacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "contacts_page")
@Getter
@Setter
@NoArgsConstructor
public class ContactsPage {
    @Id
    @Column(name = "id", nullable = false)
    private Long id = 1L;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "google_maps_url", length = 512)
    private String googleMapsUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
