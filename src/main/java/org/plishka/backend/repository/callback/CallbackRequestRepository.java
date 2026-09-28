package org.plishka.backend.repository.callback;

import org.plishka.backend.domain.callback.CallbackRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallbackRequestRepository extends JpaRepository<CallbackRequest, Long> {
    Page<CallbackRequest> findAllByUser_Id(Long userId, Pageable pageable);

    @Query("""
            select c
            from CallbackRequest c
            where :searchPattern is null
               or lower(c.name) like :searchPattern escape '!'
               or replace(replace(c.phone, ' ', ''), '-', '') like :normalizedPhoneSearchPattern escape '!'
            """)
    Page<CallbackRequest> findPageForAdminSearch(
            @Param("searchPattern") String searchPattern,
            @Param("normalizedPhoneSearchPattern") String normalizedPhoneSearchPattern,
            Pageable pageable
    );
}
