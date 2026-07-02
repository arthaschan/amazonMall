package com.yudao.module.amazon.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration tests for the Amazon Shop module covering
 * complete OAuth authorization flow, order sync pipeline, listing diagnosis,
 * and advertising rule execution with real database interactions.
 *
 * Uses TestContainers for MySQL and Redis.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Shop E2E Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShopE2ETest {

    // -----------------------------------------------------------------------
    // TestContainers
    // -----------------------------------------------------------------------

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("amazonops_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("schema/init.sql");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    // -----------------------------------------------------------------------
    // Autowired Components
    // -----------------------------------------------------------------------

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    // Mocked external dependencies
    @MockBean
    private SpApiClient spApiClient;

    @MockBean
    private AmazonOAuthClient amazonOAuthClient;

    @MockBean
    private LLMClient llmClient;

    @MockBean
    private NotificationService notificationService;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // -----------------------------------------------------------------------
    // Test 1: Shop Authorization Flow
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("E2E: Complete OAuth authorization flow with mocked Amazon")
    void testShopAuthorizationFlow() throws Exception {
        // Step 1: Initiate OAuth — get authorization URL
        ResponseEntity<Map> authUrlResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/shop/auth/initiate",
                Map.of("marketplace", "US", "sellerName", "TestSeller"),
                Map.class
        );

        assertThat(authUrlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authUrlResponse.getBody())
                .containsKey("authorizationUrl");

        String authUrl = (String) authUrlResponse.getBody().get("authorizationUrl");
        assertThat(authUrl).contains("amazon.com");

        // Step 2: Simulate Amazon OAuth callback with authorization code
        String authCode = "test-auth-code-12345";
        String state = (String) authUrlResponse.getBody().get("state");

        when(amazonOAuthClient.exchangeAuthCode(eq(authCode), anyString()))
                .thenReturn(new OAuthTokenResponse(
                        "access-token-xyz",
                        "refresh-token-abc",
                        3600
                ));

        when(amazonOAuthClient.getMerchantInfo(anyString()))
                .thenReturn(new MerchantInfo("A1234567890", "TestSeller", "US"));

        ResponseEntity<Map> callbackResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/shop/auth/callback",
                Map.of("code", authCode, "state", state),
                Map.class
        );

        assertThat(callbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(callbackResponse.getBody())
                .containsEntry("status", "authorized");

        // Step 3: Verify shop is stored in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT seller_id, marketplace_id, status FROM amazon_shop WHERE seller_id = 'A1234567890'"
             )) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("seller_id")).isEqualTo("A1234567890");
            assertThat(rs.getString("marketplace_id")).isEqualTo("ATVPDKIKX0DER");
            assertThat(rs.getString("status")).isEqualTo("AUTHORIZED");
        }

        // Step 4: Verify tokens are encrypted in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT access_token_encrypted, refresh_token_encrypted FROM amazon_shop_token WHERE seller_id = 'A1234567890'"
             )) {
            assertThat(rs.next()).isTrue();
            String accessToken = rs.getString("access_token_encrypted");
            String refreshToken = rs.getString("refresh_token_encrypted");

            assertThat(accessToken)
                    .as("Access token should be encrypted (not plaintext)")
                    .doesNotContain("access-token-xyz");
            assertThat(refreshToken)
                    .as("Refresh token should be encrypted (not plaintext)")
                    .doesNotContain("refresh-token-abc");
        }

        // Step 5: Verify shop is queryable via API
        ResponseEntity<Map> shopResponse = restTemplate.getForEntity(
                baseUrl() + "/api/v1/shop/A1234567890",
                Map.class
        );

        assertThat(shopResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shopResponse.getBody())
                .containsEntry("sellerId", "A1234567890")
                .containsEntry("marketplace", "US");
    }

    // -----------------------------------------------------------------------
    // Test 2: Order Sync E2E
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("E2E: Trigger sync → SP-API call → data in DB → queryable via API")
    void testOrderSyncE2E() throws Exception {
        // Step 1: Mock SP-API response with orders
        String ordersJson = """
                {
                  "payload": {
                    "Orders": [
                      {
                        "AmazonOrderId": "111-E2E-TEST-001",
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
                      },
                      {
                        "AmazonOrderId": "111-E2E-TEST-002",
                        "PurchaseDate": "2024-01-15T11:00:00Z",
                        "LastUpdateDate": "2024-01-16T15:00:00Z",
                        "OrderStatus": "Pending",
                        "FulfillmentChannel": "MFN",
                        "SalesChannel": "Amazon.com",
                        "OrderTotal": {"Amount": "49.99", "CurrencyCode": "USD"},
                        "NumberOfItemsShipped": 0,
                        "NumberOfItemsUnshipped": 2,
                        "MarketplaceId": "ATVPDKIKX0DER"
                      }
                    ]
                  }
                }
                """;

        when(spApiClient.getOrders(eq("ATVPDKIKX0DER"), any(Instant.class), any()))
                .thenReturn(new SpApiResponse(200, ordersJson));

        // Step 2: Trigger order sync via API
        ResponseEntity<Map> syncResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/order/sync",
                Map.of(
                        "sellerId", "A1234567890",
                        "marketplaceId", "ATVPDKIKX0DER",
                        "syncFrom", "2024-01-14T00:00:00Z"
                ),
                Map.class
        );

        assertThat(syncResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(syncResponse.getBody())
                .containsEntry("status", "completed");
        assertThat((int) syncResponse.getBody().get("ordersProcessed"))
                .isGreaterThanOrEqualTo(2);

        // Step 3: Verify orders are in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT amazon_order_id, order_status, order_total, fulfillment_channel " +
                     "FROM amazon_order WHERE amazon_order_id IN ('111-E2E-TEST-001', '111-E2E-TEST-002')"
             )) {

            int count = 0;
            while (rs.next()) {
                count++;
                String orderId = rs.getString("amazon_order_id");
                if ("111-E2E-TEST-001".equals(orderId)) {
                    assertThat(rs.getString("order_status")).isEqualTo("SHIPPED");
                    assertThat(rs.getBigDecimal("order_total")).isEqualByComparingTo("29.99");
                    assertThat(rs.getString("fulfillment_channel")).isEqualTo("AFN");
                } else if ("111-E2E-TEST-002".equals(orderId)) {
                    assertThat(rs.getString("order_status")).isEqualTo("PENDING");
                }
            }
            assertThat(count).as("Both orders should be in database").isEqualTo(2);
        }

        // Step 4: Query orders via API
        ResponseEntity<Map> queryResponse = restTemplate.getForEntity(
                baseUrl() + "/api/v1/order/list?sellerId=A1234567890&marketplaceId=ATVPDKIKX0DER",
                Map.class
        );

        assertThat(queryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> orders = (List<Map<String, Object>>) queryResponse.getBody().get("orders");
        assertThat(orders).hasSizeGreaterThanOrEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Test 3: Listing Diagnosis E2E
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("E2E: Create listing → diagnose → score returned with recommendations")
    void testListingDiagnosisE2E() throws Exception {
        // Step 1: Create a listing in the database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    INSERT INTO amazon_listing (asin, seller_id, marketplace_id, title, bullet_points,
                        backend_keywords, price, category, status)
                    VALUES ('B08E2ETEST01', 'A1234567890', 'ATVPDKIKX0DER',
                        'Generic Product Title',
                        'Short bullet 1|Short bullet 2|Short bullet 3|Bullet 4|Bullet 5',
                        'keyword1 keyword2',
                        29.99, 'Home', 'ACTIVE')
                    """);
        }

        // Step 2: Mock LLM for diagnosis
        when(llmClient.generateStructured(anyString(), any()))
                .thenReturn(Map.of(
                        "title_score", 35,
                        "bullet_score", 40,
                        "keyword_score", 30,
                        "image_score", 60,
                        "price_score", 75,
                        "overall_score", 48,
                        "recommendations", List.of(
                                "Title is too generic and lacks key product features",
                                "Bullet points are too short, should be 200-250 characters each",
                                "Backend keywords should be expanded to fill 250 bytes",
                                "Add more high-quality images showing product in use"
                        )
                ));

        // Step 3: Trigger diagnosis via API
        ResponseEntity<Map> diagnosisResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/listing/diagnose",
                Map.of("asin", "B08E2ETEST01", "sellerId", "A1234567890"),
                Map.class
        );

        assertThat(diagnosisResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> diagnosis = diagnosisResponse.getBody();
        assertThat(diagnosis).containsKey("overallScore");
        assertThat(diagnosis).containsKey("recommendations");

        int overallScore = (int) diagnosis.get("overallScore");
        assertThat(overallScore)
                .as("Poor listing should score below 60")
                .isLessThan(60);

        List<String> recommendations = (List<String>) diagnosis.get("recommendations");
        assertThat(recommendations)
                .as("Should have improvement recommendations")
                .isNotEmpty();

        // Step 4: Verify diagnosis is stored in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT asin, overall_score, diagnosed_at FROM listing_diagnosis WHERE asin = 'B08E2ETEST01'"
             )) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("asin")).isEqualTo("B08E2ETEST01");
            assertThat(rs.getInt("overall_score")).isEqualTo(overallScore);
            assertThat(rs.getTimestamp("diagnosed_at")).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // Test 4: Ad Rule Execution E2E
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("E2E: Create rule → sync data → rule fires → action logged")
    void testAdRuleExecutionE2E() throws Exception {
        // Step 1: Create an advertising rule
        ResponseEntity<Map> createRuleResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/ad/rule",
                Map.of(
                        "name", "High ACOS Auto-Reduce",
                        "sellerId", "A1234567890",
                        "condition", Map.of(
                                "metric", "ACOS",
                                "operator", "GREATER_THAN",
                                "threshold", 50.0
                        ),
                        "action", Map.of(
                                "type", "DECREASE_BID",
                                "percentage", 20.0
                        ),
                        "enabled", true
                ),
                Map.class
        );

        assertThat(createRuleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String ruleId = (String) createRuleResponse.getBody().get("ruleId");
        assertThat(ruleId).isNotNull();

        // Step 2: Create keyword with high ACOS
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    INSERT INTO ad_keyword (keyword_id, campaign_id, seller_id, keyword_text,
                        current_bid, acos, clicks, conversions, spend, sales, status)
                    VALUES ('kw-e2e-001', 'camp-e2e-001', 'A1234567890', 'expensive keyword',
                        2.50, 75.0, 100, 5, 125.00, 166.67, 'ENABLED')
                    """);
        }

        // Step 3: Mock SP-API for bid update
        when(spApiClient.updateKeywordBid(anyString(), anyString(), any()))
                .thenReturn(new SpApiResponse(200, "{\"keywordId\":\"kw-e2e-001\",\"bid\":2.00}"));

        // Step 4: Trigger rule engine execution
        ResponseEntity<Map> executeResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/ad/rule/execute",
                Map.of("sellerId", "A1234567890"),
                Map.class
        );

        assertThat(executeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> executionResult = executeResponse.getBody();
        assertThat(executionResult).containsKey("rulesEvaluated");
        assertThat(executionResult).containsKey("actionsTaken");

        int actionsTaken = (int) executionResult.get("actionsTaken");
        assertThat(actionsTaken)
                .as("At least one action should be taken for high-ACOS keyword")
                .isGreaterThanOrEqualTo(1);

        // Step 5: Verify action is logged in audit trail
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT rule_id, keyword_id, action_type, old_bid, new_bid, executed_at " +
                     "FROM ad_rule_execution_log WHERE keyword_id = 'kw-e2e-001' " +
                     "ORDER BY executed_at DESC LIMIT 1"
             )) {
            assertThat(rs.next())
                    .as("Execution log should exist for the triggered rule")
                    .isTrue();

            assertThat(rs.getString("rule_id")).isEqualTo(ruleId);
            assertThat(rs.getString("keyword_id")).isEqualTo("kw-e2e-001");
            assertThat(rs.getString("action_type")).isEqualTo("DECREASE_BID");

            double oldBid = rs.getDouble("old_bid");
            double newBid = rs.getDouble("new_bid");
            assertThat(newBid)
                    .as("New bid should be 20% less than old bid (2.50 * 0.80 = 2.00)")
                    .isCloseTo(oldBid * 0.80, within(0.01));

            assertThat(rs.getTimestamp("executed_at")).isNotNull();
        }

        // Step 6: Verify keyword bid was actually updated in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT current_bid FROM ad_keyword WHERE keyword_id = 'kw-e2e-001'"
             )) {
            assertThat(rs.next()).isTrue();
            double updatedBid = rs.getDouble("current_bid");
            assertThat(updatedBid)
                    .as("Keyword bid in DB should reflect the rule action")
                    .isCloseTo(2.00, within(0.01));
        }
    }

    // -----------------------------------------------------------------------
    // Cleanup
    // -----------------------------------------------------------------------

    @AfterEach
    void cleanup() throws Exception {
        // Clean up test data between tests (except test 1 which creates shared data)
    }

    @AfterAll
    static void cleanupAll() throws Exception {
        // TestContainers handles container shutdown automatically
    }
}
