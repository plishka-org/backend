package org.plishka.backend.repository.contacts;

import java.util.List;
import org.plishka.backend.domain.contacts.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    List<SocialLink> findAllByContactsPageIdOrderByDisplayOrderAsc(Long contactsPageId);
}
