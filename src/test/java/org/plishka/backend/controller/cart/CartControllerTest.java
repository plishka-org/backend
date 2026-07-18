package org.plishka.backend.controller.cart;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartItemSummaryDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.cart.MergeCartRequestDto;
import org.plishka.backend.dto.cart.UpdateCartItemRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.cart.CartService;
import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest extends BaseControllerTest {
    private static final long USER_ID = 1L;
    private static final long CART_ITEM_ID = 7L;
    private static final long PRODUCT_ID = 10L;
    private static final long INVALID_PRODUCT_ID = -1L;
    private static final String USER_EMAIL = "customer@example.com";
    private static final String PRODUCT_NAME = "Oak Garden Bench";
    private static final String CATEGORY_NAME = "Outdoor Tables and Benches";
    private static final long UNIT_PRICE = 450L;
    private static final long SUBTOTAL = 900L;
    private static final long TOTAL_PRICE = 900L;
    private static final int QUANTITY = 2;
    private static final int UPDATED_QUANTITY = 3;
    private static final int INVALID_QUANTITY = 0;
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product not found";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Test
    void getCart_ShouldReturnCartAndStatus200() throws Exception {
        CartSummaryDto response = cartSummary();

        when(cartService.getCart(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/cart")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].cartItemId").value(CART_ITEM_ID))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.items[0].quantity").value(QUANTITY))
                .andExpect(jsonPath("$.totalPrice").value(900));

        verify(cartService).getCart(USER_ID);
    }

    @Test
    void getCart_ShouldReturn403_WhenShopModeIsDisabled() throws Exception {
        when(shopModeService.isShopModeEnabled()).thenReturn(false);

        mockMvc.perform(get("/cart")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ShopModeService.SHOP_MODE_DISABLED_MESSAGE));

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_ShouldReturnUpdatedCartAndStatus200_WhenRequestIsValid() throws Exception {
        AddCartItemRequestDto request = addItemRequest(QUANTITY);
        CartSummaryDto response = cartSummary();

        when(cartService.addItem(eq(USER_ID), any(AddCartItemRequestDto.class))).thenReturn(response);

        performAddItem(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value(PRODUCT_NAME))
                .andExpect(jsonPath("$.totalPrice").value(900));

        verify(cartService).addItem(eq(USER_ID), any(AddCartItemRequestDto.class));
    }

    @Test
    void addItem_ShouldReturn400_WhenQuantityIsNotPositive() throws Exception {
        AddCartItemRequestDto request = addItemRequest(INVALID_QUANTITY);

        performAddItem(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_ShouldReturn404_WhenProductNotFound() throws Exception {
        AddCartItemRequestDto request = addItemRequest(QUANTITY);

        when(cartService.addItem(eq(USER_ID), any(AddCartItemRequestDto.class)))
                .thenThrow(new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        performAddItem(request)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItem_ShouldReturnUpdatedCartAndStatus200_WhenRequestIsValid() throws Exception {
        UpdateCartItemRequestDto request = updateItemRequest(UPDATED_QUANTITY);
        CartSummaryDto response = updatedCartSummary();

        when(cartService.updateItem(eq(USER_ID), eq(PRODUCT_ID), any(UpdateCartItemRequestDto.class)))
                .thenReturn(response);

        performUpdateItem(PRODUCT_ID, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(UPDATED_QUANTITY))
                .andExpect(jsonPath("$.totalPrice").value(1350));

        verify(cartService).updateItem(eq(USER_ID), eq(PRODUCT_ID), any(UpdateCartItemRequestDto.class));
    }

    @Test
    void updateItem_ShouldReturn400_WhenQuantityIsNotPositive() throws Exception {
        UpdateCartItemRequestDto request = updateItemRequest(INVALID_QUANTITY);

        performUpdateItem(PRODUCT_ID, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_ShouldReturn400_WhenProductIdIsNotPositive() throws Exception {
        UpdateCartItemRequestDto request = updateItemRequest(UPDATED_QUANTITY);

        performUpdateItem(INVALID_PRODUCT_ID, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeItem_ShouldReturnUpdatedCartAndStatus200() throws Exception {
        CartSummaryDto response = emptyCartSummary();

        when(cartService.removeItem(USER_ID, PRODUCT_ID)).thenReturn(response);

        mockMvc.perform(delete("/cart/items/{productId}", PRODUCT_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0));

        verify(cartService).removeItem(USER_ID, PRODUCT_ID);
    }

    @Test
    void clearCart_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/cart")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(cartService).clearCart(USER_ID);
    }

    @Test
    void mergeCart_ShouldReturnMergedCartAndStatus200() throws Exception {
        MergeCartRequestDto request = mergeRequest(QUANTITY);
        CartSummaryDto response = cartSummary();

        when(cartService.mergeCart(eq(USER_ID), any(MergeCartRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/cart/merge")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.totalPrice").value(900));

        verify(cartService).mergeCart(eq(USER_ID), any(MergeCartRequestDto.class));
    }

    @Test
    void mergeCart_ShouldReturn400_WhenItemsListIsMissing() throws Exception {
        mockMvc.perform(post("/cart/merge")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mergeCart_ShouldReturn400_WhenItemsListIsEmpty() throws Exception {
        mockMvc.perform(post("/cart/merge")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mergeCart_ShouldReturn400_WhenItemsListContainsNullElement() throws Exception {
        mockMvc.perform(post("/cart/merge")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mergeCart_ShouldReturn400_WhenItemQuantityIsNotPositive() throws Exception {
        MergeCartRequestDto request = mergeRequest(INVALID_QUANTITY);

        mockMvc.perform(post("/cart/merge")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mergeCart_ShouldReturn400_WhenServiceRejectsRequest() throws Exception {
        MergeCartRequestDto request = mergeRequest(QUANTITY);

        when(cartService.mergeCart(eq(USER_ID), any(MergeCartRequestDto.class)))
                .thenThrow(new BadRequestException("Cart merge failed"));

        mockMvc.perform(post("/cart/merge")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions performAddItem(AddCartItemRequestDto request) throws Exception {
        return mockMvc.perform(post("/cart/items")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performUpdateItem(Long productId, UpdateCartItemRequestDto request) throws Exception {
        return mockMvc.perform(put("/cart/items/{productId}", productId)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private static MergeCartRequestDto mergeRequest(Integer quantity) {
        return new MergeCartRequestDto(List.of(addItemRequest(quantity)));
    }

    private static AddCartItemRequestDto addItemRequest(Integer quantity) {
        return new AddCartItemRequestDto(PRODUCT_ID, quantity);
    }

    private static UpdateCartItemRequestDto updateItemRequest(Integer quantity) {
        return new UpdateCartItemRequestDto(quantity);
    }

    private static CartSummaryDto cartSummary() {
        return new CartSummaryDto(
                List.of(cartItem(QUANTITY, SUBTOTAL)),
                TOTAL_PRICE
        );
    }

    private static CartSummaryDto updatedCartSummary() {
        return new CartSummaryDto(
                List.of(cartItem(UPDATED_QUANTITY, 1350L)),
                1350L
        );
    }

    private static CartSummaryDto emptyCartSummary() {
        return new CartSummaryDto(List.of(), 0L);
    }

    private static CartItemSummaryDto cartItem(Integer quantity, Long subtotal) {
        return new CartItemSummaryDto(
                CART_ITEM_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                CATEGORY_NAME,
                quantity,
                UNIT_PRICE,
                subtotal
        );
    }
}
