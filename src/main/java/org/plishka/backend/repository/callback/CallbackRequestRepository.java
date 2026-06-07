package org.plishka.backend.repository.callback;

import org.plishka.backend.domain.callback.CallbackRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallbackRequestRepository extends JpaRepository<CallbackRequest, Long> {
    Page<CallbackRequest> findAllByUser_Id(Long userId, Pageable pageable);
}
