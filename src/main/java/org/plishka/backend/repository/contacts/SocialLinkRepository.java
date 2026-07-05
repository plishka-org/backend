package org.plishka.backend.repository.contacts;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.contacts.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    List<SocialLink> findAllByContactsPageIdOrderByUpdatedAtDescIdDesc(Long contactsPageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SocialLink s
            where s.contactsPageId = :contactsPageId
            order by s.updatedAt desc, s.id desc
            """)
    List<SocialLink> findAllByContactsPageIdForUpdateOrderByUpdatedAtDescIdDesc(
            @Param("contactsPageId") Long contactsPageId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SocialLink s
            where s.contactsPageId = :contactsPageId
            and s.id = :socialLinkId
            """)
    Optional<SocialLink> findByContactsPageIdAndIdForUpdate(
            @Param("contactsPageId") Long contactsPageId,
            @Param("socialLinkId") Long socialLinkId
    );
}
