package org.plishka.backend.repository.contacts;

import org.plishka.backend.domain.contacts.ContactsPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactsPageRepository extends JpaRepository<ContactsPage, Long> {
}
