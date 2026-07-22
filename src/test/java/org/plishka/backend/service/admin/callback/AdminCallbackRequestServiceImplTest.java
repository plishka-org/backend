package org.plishka.backend.service.admin.callback;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.callback.CallbackRequest;
import org.plishka.backend.dto.admin.callback.AdminCallbackRequestSearchRequestDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.mapper.callback.CallbackRequestMapper;
import org.plishka.backend.repository.callback.CallbackRequestRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCallbackRequestServiceImplTest {
    @Mock
    private CallbackRequestRepository callbackRequestRepository;

    @Mock
    private CallbackRequestMapper callbackRequestMapper;

    @InjectMocks
    private AdminCallbackRequestServiceImpl service;

    @Test
    void getCallbackRequests_ShouldNormalizePhoneSearch() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setId(1L);
        CallbackRequestDto dto = new CallbackRequestDto(
                1L,
                "Olena",
                "+380501112233",
                "Call me",
                Instant.parse("2026-07-01T12:00:00Z")
        );
        when(callbackRequestRepository.findPageForAdminSearch(eq("%050 -%"), eq("%050%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(callbackRequest)));
        when(callbackRequestMapper.toDto(callbackRequest)).thenReturn(dto);

        assertEquals(
                List.of(dto),
                service.getCallbackRequests(new AdminCallbackRequestSearchRequestDto(" 050 - ", null), 0, 10).content()
        );
    }

    @Test
    void getCallbackRequests_ShouldEscapeLikeWildcards() {
        when(callbackRequestRepository.findPageForAdminSearch(eq("%!%!_!!%"), eq("%!%!_!!%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getCallbackRequests(new AdminCallbackRequestSearchRequestDto(" %_! ", null), 0, 10);

        verify(callbackRequestRepository).findPageForAdminSearch(
                eq("%!%!_!!%"),
                eq("%!%!_!!%"),
                any(Pageable.class)
        );
    }

    @Test
    void getCallbackRequests_ShouldUseDefaultCreatedAtDescendingSort() {
        when(callbackRequestRepository.findPageForAdminSearch(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getCallbackRequests(new AdminCallbackRequestSearchRequestDto(null, null), 0, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(callbackRequestRepository).findPageForAdminSearch(any(), any(), pageableCaptor.capture());
        assertEquals("DESC", pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection().name());
    }

    @Test
    void getCallbackRequests_ShouldRejectUnsupportedSort() {
        assertThrows(
                BadRequestException.class,
                () -> service.getCallbackRequests(new AdminCallbackRequestSearchRequestDto(null, "name,asc"), 0, 10)
        );

        verifyNoInteractions(callbackRequestRepository);
    }
}
