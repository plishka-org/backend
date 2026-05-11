package org.plishka.backend.domain.about;

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
@Table(name = "about_page_content")
@Getter
@Setter
@NoArgsConstructor
public class AboutPageContent {
    @Id
    @Column(name = "id", nullable = false)
    private Long id = 1L;

    @Column(name = "history_text", columnDefinition = "TEXT", nullable = false)
    private String historyText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
