package org.plishka.backend.service.admin.callback;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.callback.CallbackRequest;
import org.plishka.backend.dto.admin.callback.AdminCallbackRequestSearchRequestDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.repository.callback.CallbackRequestRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminCallbackRequestServiceImplIntegrationTest {
    @Autowired
    private AdminCallbackRequestService adminCallbackRequestService;

    @Autowired
    private CallbackRequestRepository callbackRequestRepository;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void getCallbackRequests_ShouldTreatLikeWildcardsAsLiteralText() {
        CallbackRequest percentMatch = createCallbackRequest("Customer % Literal", "+380501112231");
        CallbackRequest underscoreMatch = createCallbackRequest("Customer _ Literal", "+380501112232");
        createCallbackRequest("Regular Customer", "+380501112233");

        assertEquals(List.of(percentMatch.getId()), findCallbackRequestIds("%"));
        assertEquals(List.of(underscoreMatch.getId()), findCallbackRequestIds("_"));
    }

    @Test
    void getCallbackRequests_ShouldSearchByNormalizedPhoneContains() {
        CallbackRequest phoneMatch = createCallbackRequest("Phone Search", "+380501112234");
        createCallbackRequest("Other Phone", "+380671112235");

        assertEquals(List.of(phoneMatch.getId()), findCallbackRequestIds("050 111"));
    }

    private List<Long> findCallbackRequestIds(String searchQuery) {
        return adminCallbackRequestService.getCallbackRequests(
                        new AdminCallbackRequestSearchRequestDto(searchQuery, "createdAt,asc"),
                        0,
                        10
                )
                .content()
                .stream()
                .map(CallbackRequestDto::callbackRequestId)
                .toList();
    }

    private CallbackRequest createCallbackRequest(String name, String phone) {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setName(name);
        callbackRequest.setPhone(phone);
        callbackRequest.setMessage("Please call me");
        return callbackRequestRepository.saveAndFlush(callbackRequest);
    }
}
