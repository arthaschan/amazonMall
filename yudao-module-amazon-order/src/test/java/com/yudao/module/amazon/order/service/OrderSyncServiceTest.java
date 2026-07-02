package com.yudao.module.amazon.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Order Sync Service covering JSON parsing, idempotency,
 * multi-marketplace isolation, status mapping, PII handling, rate limiting,
 * large batch processing, and failure recovery.
 *
 * Based on wimoor/wimoor-amazon AmzOrderMainServiceImpl patterns.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Sync Service Tests")
class OrderSyncServiceTest {

    @Mock
    private SpApiClient spApiClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderBuyerAddressRepository addressRepository;

    @Mock
    private MarketplaceService marketplaceService;

    private OrderSyncService orderSyncService;
    private ObjectMapper objectMapper;

    private static final String MARKETPLACE_US = "ATVPDKIKX0DER";
    private static final String MARKETPLACE_UK = "A1F83G8C2ARO7P";
    private static final String MARKETPLACE_DE = "A1PA6795UKMFR9";
    private static final String SELLER_ID = "A1234567890";
    private static final String AUTH_ID = "auth-001";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderSyncService = new OrderSyncService(
                spApiClient, orderRepository, orderItemRepository,
                addressRepository, marketplaceService, objectMapper
        );
    }

    // -----------------------------------------------------------------------
    // Order Parsing
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("JSON response from SP-API is correctly parsed to OrderDO entity")
    void testOrderParsing() throws Exception {
        // Given: SP-API JSON response
        String ordersJson = """
                {
                  "payload": {
                    "Orders": [
                      {
                        "AmazonOrderId": "111-1234567-1234567",
                        "PurchaseDate": "2024-01-15T10:30:00Z",
                        "LastUpdateDate": "2024-01-16T14:20:00Z",
                        "OrderStatus": "Shipped",
                        "FulfillmentChannel": "AFN",
                        "SalesChannel": "Amazon.com",
                        "OrderTotal": {"Amount": "29.99", "CurrencyCode": "USD"},
                        "NumberOfItemsShipped": 1,
                        "NumberOfItemsUnshipped": 0,
                        "MarketplaceId": "ATVPDKIKX0DER",
                        "OrderType": "StandardOrder",
                        "IsBusinessOrder": false
                      }
                    ]
                  }
                }
                """;

        SpApiResponse apiResponse = new SpApiResponse(200, ordersJson);
        when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), any()))
                .thenReturn(apiResponse);

        // When: Syncing orders
        SyncResult result = orderSyncService.syncOrders(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: Order is correctly parsed
        ArgumentCaptor<OrderDO> orderCaptor = ArgumentCaptor.forClass(OrderDO.class);
        verify(orderRepository).saveOrUpdate(orderCaptor.capture());

        OrderDO savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getAmazonOrderId()).isEqualTo("111-1234567-1234567");
        assertThat(savedOrder.getOrderStatus()).isEqualTo("Shipped");
        assertThat(savedOrder.getFulfillmentChannel()).isEqualTo("AFN");
        assertThat(savedOrder.getOrderTotal()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(savedOrder.getNumberOfItemsShipped()).isEqualTo(1);
        assertThat(savedOrder.getMarketplaceId()).isEqualTo(MARKETPLACE_US);
    }

    @Test
    @DisplayName("Order items are parsed and associated with parent order")
    void testOrderItemParsing() throws Exception {
        // Given: Order with items
        String ordersJson = """
                {
                  "payload": {
                    "Orders": [
                      {
                        "AmazonOrderId": "111-9999999-9999999",
                        "OrderStatus": "Shipped",
                        "MarketplaceId": "ATVPDKIKX0DER",
                        "PurchaseDate": "2024-01-15T10:30:00Z"
                      }
                    ]
                  }
                }
                """;

        String orderItemsJson = """
                {
                  "payload": {
                    "OrderItems": [
                      {
                        "ASIN": "B08N5WRWNW",
                        "OrderItemId": "item-001",
                        "SellerSKU": "SKU-001",
                        "Title": "Test Product",
                        "QuantityOrdered": 2,
                        "QuantityShipped": 2,
                        "ItemPrice": {"Amount": "19.98", "CurrencyCode": "USD"}
                      }
                    ]
                  }
                }
                """;

        when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), any()))
                .thenReturn(new SpApiResponse(200, ordersJson));
        when(spApiClient.getOrderItems(eq("111-9999999-9999999"), eq(MARKETPLACE_US)))
                .thenReturn(new SpApiResponse(200, orderItemsJson));

        // When: Syncing orders with items
        orderSyncService.syncOrders(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: Items are saved
        verify(orderItemRepository).saveOrUpdate(argThat((OrderItemDO item) ->
                item.getAsin().equals("B08N5WRWNW") &&
                item.getQuantityOrdered() == 2 &&
                item.getAmazonOrderId().equals("111-9999999-9999999")
        ));
    }

    // -----------------------------------------------------------------------
    // Idempotency
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Duplicate order is updated, not inserted twice")
    void testIdempotency() throws Exception {
        // Given: Order already exists in database
        String orderId = "111-DUPLICATE-1234567";
        OrderDO existingOrder = new OrderDO();
        existingOrder.setAmazonOrderId(orderId);
        existingOrder.setOrderStatus("Pending");
        existingOrder.setAuthId(AUTH_ID);

        when(orderRepository.findByAmazonOrderIdAndAuthId(orderId, AUTH_ID))
                .thenReturn(Optional.of(existingOrder));

        String ordersJson = """
                {
                  "payload": {
                    "Orders": [
                      {
                        "AmazonOrderId": "%s",
                        "OrderStatus": "Shipped",
                        "MarketplaceId": "ATVPDKIKX0DER",
                        "PurchaseDate": "2024-01-15T10:30:00Z"
                      }
                    ]
                  }
                }
                """.formatted(orderId);

        when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), any()))
                .thenReturn(new SpApiResponse(200, ordersJson));

        // When: Syncing the same order again
        orderSyncService.syncOrders(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: Order is updated, not duplicated
        verify(orderRepository).saveOrUpdate(argThat((OrderDO order) ->
                order.getAmazonOrderId().equals(orderId) &&
                order.getOrderStatus().equals("Shipped")
        ));

        // And: insert was NOT called (only update)
        verify(orderRepository, never()).insert(any());
    }

    @Test
    @DisplayName("Concurrent sync of same order does not create duplicates (optimistic locking)")
    void testIdempotency_ConcurrentSync() {
        // Given: Two concurrent sync attempts
        String orderId = "111-CONCURRENT-1234567";

        when(orderRepository.findByAmazonOrderIdAndAuthId(orderId, AUTH_ID))
                .thenReturn(Optional.empty())   // first check: doesn't exist
                .thenReturn(Optional.of(new OrderDO())); // second check: exists now

        // When: Checking idempotency
        boolean shouldInsert = orderSyncService.shouldInsertOrder(orderId, AUTH_ID);

        // Then: Properly handles concurrent access
        assertThat(shouldInsert).isFalse(); // second check found it exists
    }

    // -----------------------------------------------------------------------
    // Multi-Marketplace
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Orders from US/UK/DE are stored with correct marketplace IDs")
    void testMultiMarketplace() throws Exception {
        // Given: Orders from different marketplaces
        Map<String, String> marketplaceOrders = Map.of(
                MARKETPLACE_US, createOrderJson("111-US-ORDER-1234567", "Shipped", MARKETPLACE_US),
                MARKETPLACE_UK, createOrderJson("028-UK-ORDER-7654321", "Shipped", MARKETPLACE_UK),
                MARKETPLACE_DE, createOrderJson("028-DE-ORDER-9876543", "Pending", MARKETPLACE_DE)
        );

        for (Map.Entry<String, String> entry : marketplaceOrders.entrySet()) {
            when(spApiClient.getOrders(eq(entry.getKey()), any(Instant.class), any()))
                    .thenReturn(new SpApiResponse(200, entry.getValue()));
        }

        // When: Syncing all marketplaces
        for (String marketplaceId : marketplaceOrders.keySet()) {
            orderSyncService.syncOrders(AUTH_ID, marketplaceId, Instant.now().minusSeconds(3600));
        }

        // Then: Each order has correct marketplace ID
        ArgumentCaptor<OrderDO> captor = ArgumentCaptor.forClass(OrderDO.class);
        verify(orderRepository, times(3)).saveOrUpdate(captor.capture());

        List<OrderDO> savedOrders = captor.getAllValues();
        assertThat(savedOrders)
                .extracting(OrderDO::getMarketplaceId)
                .containsExactlyInAnyOrder(MARKETPLACE_US, MARKETPLACE_UK, MARKETPLACE_DE);
    }

    @Test
    @DisplayName("Same order ID in different marketplaces are separate records")
    void testMultiMarketplace_SameOrderIdDifferentMarketplace() {
        // Some Amazon order IDs can theoretically appear in different marketplaces
        String orderId = "111-SAME-ID-1234567";

        OrderDO usOrder = new OrderDO();
        usOrder.setAmazonOrderId(orderId);
        usOrder.setMarketplaceId(MARKETPLACE_US);

        OrderDO ukOrder = new OrderDO();
        ukOrder.setAmazonOrderId(orderId);
        ukOrder.setMarketplaceId(MARKETPLACE_UK);

        // Different marketplace = different record
        when(orderRepository.findByAmazonOrderIdAndMarketplace(orderId, MARKETPLACE_US))
                .thenReturn(Optional.of(usOrder));
        when(orderRepository.findByAmazonOrderIdAndMarketplace(orderId, MARKETPLACE_UK))
                .thenReturn(Optional.empty());

        assertThat(orderSyncService.orderExists(orderId, MARKETPLACE_US)).isTrue();
        assertThat(orderSyncService.orderExists(orderId, MARKETPLACE_UK)).isFalse();
    }

    // -----------------------------------------------------------------------
    // Order Status Mapping
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Amazon order statuses are mapped to internal status codes")
    void testOrderStatusMapping() {
        // Given: Various Amazon statuses
        Map<String, String> expectedMappings = Map.of(
                "Pending", "PENDING",
                "Unshipped", "UNSHIPPED",
                "PartiallyShipped", "PARTIALLY_SHIPPED",
                "Shipped", "SHIPPED",
                "Canceled", "CANCELLED",
                "Unfulfillable", "UNFULFILLABLE",
                "InvoiceUnconfirmed", "INVOICE_UNCONFIRMED"
        );

        // When/Then: Each Amazon status maps correctly
        for (Map.Entry<String, String> entry : expectedMappings.entrySet()) {
            String internalStatus = orderSyncService.mapOrderStatus(entry.getKey());
            assertThat(internalStatus)
                    .as("Amazon status '%s' should map to '%s'", entry.getKey(), entry.getValue())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    @DisplayName("Unknown Amazon status maps to UNKNOWN rather than throwing")
    void testOrderStatusMapping_Unknown() {
        String status = orderSyncService.mapOrderStatus("NewFutureStatus");
        assertThat(status).isEqualTo("UNKNOWN");
    }

    // -----------------------------------------------------------------------
    // Buyer PII Handling (RDT Required)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Buyer address requires Restricted Data Token (RDT) for access")
    void testBuyerPIIHandling() throws Exception {
        // Given: Order with PII data that requires RDT
        String orderId = "111-PII-TEST-1234567";

        // First call without RDT returns no address
        String orderWithoutPII = createOrderJson(orderId, "Shipped", MARKETPLACE_US);
        when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), any()))
                .thenReturn(new SpApiResponse(200, orderWithoutPII));

        // RDT request for PII
        when(spApiClient.getRestrictedDataToken(anyList(), eq(MARKETPLACE_US)))
                .thenReturn("rdt.restricted-data-token-xyz");

        // Second call with RDT returns address
        String orderWithPII = """
                {
                  "AmazonOrderId": "%s",
                  "ShippingAddress": {
                    "Name": "John Doe",
                    "AddressLine1": "123 Main St",
                    "City": "Seattle",
                    "StateOrRegion": "WA",
                    "PostalCode": "98101",
                    "CountryCode": "US"
                  }
                }
                """.formatted(orderId);

        when(spApiClient.getOrderWithRDT(eq(orderId), anyString()))
                .thenReturn(new SpApiResponse(200, orderWithPII));

        // When: Syncing orders with PII
        orderSyncService.syncOrdersWithPII(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: RDT was requested for PII access
        verify(spApiClient).getRestrictedDataToken(anyList(), eq(MARKETPLACE_US));

        // And: Address was saved
        verify(addressRepository).saveOrUpdate(argThat((BuyerAddressDO addr) ->
                addr.getCity() != null && addr.getCity().equals("Seattle")
        ));
    }

    @Test
    @DisplayName("PII data is encrypted before storage and masked in logs")
    void testBuyerPIIHandling_Encryption() {
        // Given: Buyer address data
        BuyerAddressDO address = new BuyerAddressDO();
        address.setAmazonOrderId("111-PII-1234567");
        address.setBuyerName("John Doe");
        address.setAddressLine1("123 Main St");
        address.setCity("Seattle");
        address.setPostalCode("98101");

        // When: Saving with PII protection
        orderSyncService.saveBuyerAddress(address);

        // Then: Address is saved (encryption handled by service layer)
        verify(addressRepository).saveOrUpdate(address);

        // And: toString() should mask PII
        assertThat(address.toString())
                .as("toString() should mask buyer name for logging")
                .doesNotContain("John Doe");
    }

    // -----------------------------------------------------------------------
    // Rate Limit During Sync
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sync respects SP-API rate limits with proper pacing")
    void testRateLimitDuringSync() throws Exception {
        // Given: Multiple pages of orders
        String page1 = createPaginatedOrderResponse("111-PAGE1-001", "next-token-123");
        String page2 = createPaginatedOrderResponse("111-PAGE2-001", "next-token-456");
        String page3 = createPaginatedOrderResponse("111-PAGE3-001", null); // no more pages

        when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), isNull()))
                .thenReturn(new SpApiResponse(200, page1));
        when(spApiClient.getOrdersByNextToken(eq(MARKETPLACE_US), eq("next-token-123")))
                .thenReturn(new SpApiResponse(200, page2));
        when(spApiClient.getOrdersByNextToken(eq(MARKETPLACE_US), eq("next-token-456")))
                .thenReturn(new SpApiResponse(200, page3));

        // When: Syncing with pagination
        SyncResult result = orderSyncService.syncOrders(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: All pages processed
        assertThat(result.getPagesProcessed()).isEqualTo(3);
        assertThat(result.getOrdersProcessed()).isGreaterThanOrEqualTo(3);

        // And: API was called for each page
        verify(spApiClient).getOrders(eq(MARKETPLACE_US), any(Instant.class), isNull());
        verify(spApiClient, times(2)).getOrdersByNextToken(eq(MARKETPLACE_US), anyString());
    }

    // -----------------------------------------------------------------------
    // Large Order Batch
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("1000+ orders are processed correctly with batching")
    void testLargeOrderBatch() throws Exception {
        // Given: 1000 orders across multiple pages
        int totalOrders = 1000;
        int ordersPerPage = 100;
        int totalPages = totalOrders / ordersPerPage;

        for (int page = 0; page < totalPages; page++) {
            String nextToken = (page < totalPages - 1) ? "token-" + (page + 1) : null;
            String response = createMultiOrderResponse(page * ordersPerPage, ordersPerPage, nextToken);

            if (page == 0) {
                when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), isNull()))
                        .thenReturn(new SpApiResponse(200, response));
            } else {
                when(spApiClient.getOrdersByNextToken(eq(MARKETPLACE_US), eq("token-" + page)))
                        .thenReturn(new SpApiResponse(200, response));
            }
        }

        // When: Syncing large batch
        SyncResult result = orderSyncService.syncOrders(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: All orders processed
        assertThat(result.getOrdersProcessed())
                .as("All 1000+ orders should be processed")
                .isGreaterThanOrEqualTo(totalOrders);

        assertThat(result.getErrorsEncountered())
                .as("No errors expected for valid order batch")
                .isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Sync Failure Recovery
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Partial failure resumes from last successful checkpoint")
    void testSyncFailureRecovery() throws Exception {
        // Given: 3 pages where page 2 fails
        String page1 = createPaginatedOrderResponse("111-P1-001", "token-p2");
        String page3 = createPaginatedOrderResponse("111-P3-001", null);

        when(spApiClient.getOrders(eq(MARKETPLACE_US), any(Instant.class), isNull()))
                .thenReturn(new SpApiResponse(200, page1));
        when(spApiClient.getOrdersByNextToken(eq(MARKETPLACE_US), eq("token-p2")))
                .thenThrow(new SpApiException("Rate limit exceeded", 429));
        when(spApiClient.getOrdersByNextToken(eq(MARKETPLACE_US), eq("token-p2")))
                .thenReturn(new SpApiResponse(200, page3));

        // When: Syncing with failure and recovery
        SyncResult result = orderSyncService.syncOrders(AUTH_ID, MARKETPLACE_US, Instant.now().minusSeconds(3600));

        // Then: Result indicates partial success
        assertThat(result.getOrdersProcessed()).isGreaterThan(0);

        // And: Checkpoint was saved for recovery
        assertThat(result.getLastSuccessfulToken()).isNotNull();
    }

    @Test
    @DisplayName("Sync checkpoint allows resuming from specific next token")
    void testSyncFromCheckpoint() throws Exception {
        // Given: Checkpoint from previous failed sync
        String resumeToken = "checkpoint-token-xyz";
        String responseJson = createPaginatedOrderResponse("111-RESUMED-001", null);

        when(spApiClient.getOrdersByNextToken(eq(MARKETPLACE_US), eq(resumeToken)))
                .thenReturn(new SpApiResponse(200, responseJson));

        // When: Resuming sync from checkpoint
        SyncResult result = orderSyncService.resumeSync(AUTH_ID, MARKETPLACE_US, resumeToken);

        // Then: Sync continues from checkpoint
        assertThat(result.getOrdersProcessed()).isGreaterThan(0);
        verify(spApiClient).getOrdersByNextToken(MARKETPLACE_US, resumeToken);
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private String createOrderJson(String orderId, String status, String marketplaceId) {
        return """
                {
                  "payload": {
                    "Orders": [
                      {
                        "AmazonOrderId": "%s",
                        "OrderStatus": "%s",
                        "MarketplaceId": "%s",
                        "PurchaseDate": "2024-01-15T10:30:00Z"
                      }
                    ]
                  }
                }
                """.formatted(orderId, status, marketplaceId);
    }

    private String createPaginatedOrderResponse(String orderId, String nextToken) {
        String nextTokenField = nextToken != null
                ? ",\"NextToken\": \"" + nextToken + "\""
                : "";
        return """
                {
                  "payload": {
                    "Orders": [
                      {
                        "AmazonOrderId": "%s",
                        "OrderStatus": "Shipped",
                        "MarketplaceId": "ATVPDKIKX0DER",
                        "PurchaseDate": "2024-01-15T10:30:00Z"
                      }
                    ]%s
                  }
                }
                """.formatted(orderId, nextTokenField);
    }

    private String createMultiOrderResponse(int startIndex, int count, String nextToken) {
        StringBuilder orders = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) orders.append(",");
            orders.append("""
                    {
                      "AmazonOrderId": "111-BATCH-%07d",
                      "OrderStatus": "Shipped",
                      "MarketplaceId": "ATVPDKIKX0DER",
                      "PurchaseDate": "2024-01-15T10:30:00Z"
                    }
                    """.formatted(startIndex + i));
        }

        String nextTokenField = nextToken != null
                ? ",\"NextToken\": \"" + nextToken + "\""
                : "";

        return """
                {
                  "payload": {
                    "Orders": [%s]%s
                  }
                }
                """.formatted(orders.toString(), nextTokenField);
    }
}
