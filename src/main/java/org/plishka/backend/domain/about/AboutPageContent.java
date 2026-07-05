package org.plishka.backend.domain.about;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "about_page_content")
@Getter
@Setter
@NoArgsConstructor
public class AboutPageContent {
    @Id
    @Column(name = "id", nullable = false)
    private Long id = 1L;

    @Column(name = "main_title", nullable = false, length = 255)
    private String mainTitle;

    @Column(name = "main_subtitle", columnDefinition = "TEXT", nullable = false)
    private String mainSubtitle;

    @Column(name = "secondary_title", nullable = false, length = 255)
    private String secondaryTitle;

    @Column(name = "secondary_subtitle", columnDefinition = "TEXT", nullable = false)
    private String secondarySubtitle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
