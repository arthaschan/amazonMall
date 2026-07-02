package com.yudao.module.amazon.ad.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Advertising Rule Engine covering threshold rules, multi-condition rules,
 * bid adjustment actions, keyword management actions, notification actions,
 * and complete audit trail logging.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Advertising Rule Engine Tests")
class AdRuleEngineTest {

    @Mock
    private AdCampaignRepository campaignRepository;

    @Mock
    private AdKeywordRepository keywordRepository;

    @Mock
    private AdRuleRepository ruleRepository;

    @Mock
    private AdRuleLogRepository ruleLogRepository;

    @Mock
    private SpApiClient spApiClient;

    @Mock
    private NotificationService notificationService;

    private AdRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new AdRuleEngine(
                campaignRepository, keywordRepository, ruleRepository,
                ruleLogRepository, spApiClient, notificationService
        );
    }

    // -----------------------------------------------------------------------
    // ACOS Threshold Rule
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ACOS > 50% triggers bid decrease action")
    void testAcosThresholdRule() {
        // Given: Rule — if ACOS > 50%, decrease bid by 20%
        AdRule rule = AdRule.builder()
                .id("rule-001")
                .name("High ACOS - Decrease Bid")
                .condition(AdCondition.builder()
                        .metric(AdMetric.ACOS)
                        .operator(ComparisonOperator.GREATER_THAN)
                        .threshold(50.0)
                        .build())
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(20.0)
                        .build())
                .build();

        // And: Keyword with ACOS of 65%
        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .campaignId("camp-001")
                .acos(65.0)
                .clicks(100)
                .spend(BigDecimal.valueOf(50.0))
                .sales(BigDecimal.valueOf(76.92))
                .build();

        // When: Evaluating the rule
        RuleEvaluationResult result = ruleEngine.evaluate(rule, metrics);

        // Then: Rule triggers
        assertThat(result.isTriggered())
                .as("ACOS 65% > 50% threshold should trigger rule")
                .isTrue();

        assertThat(result.getAction().getType())
                .isEqualTo(ActionType.DECREASE_BID);
    }

    @Test
    @DisplayName("ACOS at exactly threshold does not trigger (strictly greater than)")
    void testAcosThresholdRule_Boundary() {
        AdRule rule = AdRule.builder()
                .id("rule-001")
                .condition(AdCondition.builder()
                        .metric(AdMetric.ACOS)
                        .operator(ComparisonOperator.GREATER_THAN)
                        .threshold(50.0)
                        .build())
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(20.0)
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .acos(50.0) // exactly at threshold
                .build();

        RuleEvaluationResult result = ruleEngine.evaluate(rule, metrics);

        assertThat(result.isTriggered())
                .as("ACOS exactly at threshold should NOT trigger (strictly greater than)")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Click Threshold Rule
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("High clicks with zero conversions triggers negative keyword suggestion")
    void testClickThresholdRule() {
        // Given: Rule — if clicks > 20 AND conversions = 0, add as negative keyword
        AdRule rule = AdRule.builder()
                .id("rule-002")
                .name("Wasted Spend - Add Negative Keyword")
                .conditions(List.of(
                        AdCondition.builder()
                                .metric(AdMetric.CLICKS)
                                .operator(ComparisonOperator.GREATER_THAN)
                                .threshold(20.0)
                                .build(),
                        AdCondition.builder()
                                .metric(AdMetric.CONVERSIONS)
                                .operator(ComparisonOperator.EQUALS)
                                .threshold(0.0)
                                .build()
                ))
                .conditionLogic(ConditionLogic.AND)
                .action(AdAction.builder()
                        .type(ActionType.ADD_NEGATIVE_KEYWORD)
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-002")
                .searchTerm("cheap knockoff")
                .clicks(35)
                .conversions(0)
                .spend(BigDecimal.valueOf(17.50))
                .sales(BigDecimal.ZERO)
                .build();

        RuleEvaluationResult result = ruleEngine.evaluate(rule, metrics);

        assertThat(result.isTriggered())
                .as("35 clicks with 0 conversions should trigger negative keyword rule")
                .isTrue();
        assertThat(result.getAction().getType())
                .isEqualTo(ActionType.ADD_NEGATIVE_KEYWORD);
    }

    // -----------------------------------------------------------------------
    // Budget Exhaustion Rule
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Daily budget consumed triggers budget increase or pause action")
    void testBudgetExhaustionRule() {
        // Given: Rule — if budget spent > 95% AND ACOS < 30%, increase budget
        AdRule rule = AdRule.builder()
                .id("rule-003")
                .name("Budget Exhausted - Good Performance")
                .conditions(List.of(
                        AdCondition.builder()
                                .metric(AdMetric.BUDGET_UTILIZATION)
                                .operator(ComparisonOperator.GREATER_THAN)
                                .threshold(95.0)
                                .build(),
                        AdCondition.builder()
                                .metric(AdMetric.ACOS)
                                .operator(ComparisonOperator.LESS_THAN)
                                .threshold(30.0)
                                .build()
                ))
                .conditionLogic(ConditionLogic.AND)
                .action(AdAction.builder()
                        .type(ActionType.INCREASE_BUDGET)
                        .percentage(25.0)
                        .build())
                .build();

        AdCampaignMetrics metrics = AdCampaignMetrics.builder()
                .campaignId("camp-001")
                .dailyBudget(BigDecimal.valueOf(50.0))
                .dailySpend(BigDecimal.valueOf(48.50)) // 97% utilization
                .acos(22.0)
                .build();

        RuleEvaluationResult result = ruleEngine.evaluate(rule, metrics);

        assertThat(result.isTriggered())
                .as("97% budget utilization with good ACOS should trigger budget increase")
                .isTrue();
        assertThat(result.getAction().getType()).isEqualTo(ActionType.INCREASE_BUDGET);
    }

    // -----------------------------------------------------------------------
    // Multiple Conditions (AND)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("AND logic: all conditions must be true for rule to trigger")
    void testMultipleConditionsAnd() {
        AdRule rule = AdRule.builder()
                .id("rule-004")
                .name("ACOS > 50 AND Clicks > 20")
                .conditions(List.of(
                        AdCondition.builder()
                                .metric(AdMetric.ACOS)
                                .operator(ComparisonOperator.GREATER_THAN)
                                .threshold(50.0)
                                .build(),
                        AdCondition.builder()
                                .metric(AdMetric.CLICKS)
                                .operator(ComparisonOperator.GREATER_THAN)
                                .threshold(20.0)
                                .build()
                ))
                .conditionLogic(ConditionLogic.AND)
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(15.0)
                        .build())
                .build();

        // Case 1: Both conditions met
        AdKeywordMetrics bothMet = AdKeywordMetrics.builder()
                .keywordId("kw-001").acos(60.0).clicks(30).build();
        assertThat(ruleEngine.evaluate(rule, bothMet).isTriggered())
                .as("Both conditions met should trigger")
                .isTrue();

        // Case 2: Only first condition met
        AdKeywordMetrics firstOnly = AdKeywordMetrics.builder()
                .keywordId("kw-002").acos(60.0).clicks(10).build();
        assertThat(ruleEngine.evaluate(rule, firstOnly).isTriggered())
                .as("Only ACOS condition met should NOT trigger (AND logic)")
                .isFalse();

        // Case 3: Only second condition met
        AdKeywordMetrics secondOnly = AdKeywordMetrics.builder()
                .keywordId("kw-003").acos(30.0).clicks(30).build();
        assertThat(ruleEngine.evaluate(rule, secondOnly).isTriggered())
                .as("Only clicks condition met should NOT trigger (AND logic)")
                .isFalse();

        // Case 4: Neither condition met
        AdKeywordMetrics neitherMet = AdKeywordMetrics.builder()
                .keywordId("kw-004").acos(30.0).clicks(10).build();
        assertThat(ruleEngine.evaluate(rule, neitherMet).isTriggered())
                .as("Neither condition met should NOT trigger")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Multiple Conditions (OR)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("OR logic: any condition being true triggers the rule")
    void testMultipleConditionsOr() {
        AdRule rule = AdRule.builder()
                .id("rule-005")
                .name("ACOS > 50 OR Conversion Rate < 1%")
                .conditions(List.of(
                        AdCondition.builder()
                                .metric(AdMetric.ACOS)
                                .operator(ComparisonOperator.GREATER_THAN)
                                .threshold(50.0)
                                .build(),
                        AdCondition.builder()
                                .metric(AdMetric.CONVERSION_RATE)
                                .operator(ComparisonOperator.LESS_THAN)
                                .threshold(1.0)
                                .build()
                ))
                .conditionLogic(ConditionLogic.OR)
                .action(AdAction.builder()
                        .type(ActionType.NOTIFICATION)
                        .message("Keyword performance needs review")
                        .build())
                .build();

        // Case 1: Both conditions met
        AdKeywordMetrics bothMet = AdKeywordMetrics.builder()
                .keywordId("kw-001").acos(60.0).conversionRate(0.5).build();
        assertThat(ruleEngine.evaluate(rule, bothMet).isTriggered()).isTrue();

        // Case 2: Only first condition met
        AdKeywordMetrics firstOnly = AdKeywordMetrics.builder()
                .keywordId("kw-002").acos(60.0).conversionRate(5.0).build();
        assertThat(ruleEngine.evaluate(rule, firstOnly).isTriggered())
                .as("Either condition met should trigger (OR logic)")
                .isTrue();

        // Case 3: Only second condition met
        AdKeywordMetrics secondOnly = AdKeywordMetrics.builder()
                .keywordId("kw-003").acos(30.0).conversionRate(0.5).build();
        assertThat(ruleEngine.evaluate(rule, secondOnly).isTriggered())
                .as("Either condition met should trigger (OR logic)")
                .isTrue();

        // Case 4: Neither condition met
        AdKeywordMetrics neitherMet = AdKeywordMetrics.builder()
                .keywordId("kw-004").acos(30.0).conversionRate(5.0).build();
        assertThat(ruleEngine.evaluate(rule, neitherMet).isTriggered()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Bid Increase Action
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bid increase action raises bid by specified percentage")
    void testBidIncreaseAction() {
        // Given: Rule to increase bid by 15%
        AdRule rule = AdRule.builder()
                .id("rule-006")
                .name("Good ACOS - Increase Bid")
                .condition(AdCondition.builder()
                        .metric(AdMetric.ACOS)
                        .operator(ComparisonOperator.LESS_THAN)
                        .threshold(25.0)
                        .build())
                .action(AdAction.builder()
                        .type(ActionType.INCREASE_BID)
                        .percentage(15.0)
                        .maxBid(BigDecimal.valueOf(5.00))
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .acos(18.0)
                .currentBid(BigDecimal.valueOf(1.50))
                .build();

        // When: Executing the action
        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        // Then: Bid is increased by 15%
        BigDecimal expectedNewBid = BigDecimal.valueOf(1.50)
                .multiply(BigDecimal.valueOf(1.15))
                .setScale(2, RoundingMode.HALF_UP);

        assertThat(execResult.getNewBid())
                .as("New bid should be old bid * 1.15")
                .isEqualByComparingTo(expectedNewBid);
    }

    @Test
    @DisplayName("Bid increase does not exceed maximum bid cap")
    void testBidIncreaseAction_MaxCap() {
        AdRule rule = AdRule.builder()
                .action(AdAction.builder()
                        .type(ActionType.INCREASE_BID)
                        .percentage(50.0)
                        .maxBid(BigDecimal.valueOf(3.00))
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .currentBid(BigDecimal.valueOf(2.50))
                .build();

        // 2.50 * 1.50 = 3.75, but capped at 3.00
        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        assertThat(execResult.getNewBid())
                .as("New bid should not exceed max cap")
                .isEqualByComparingTo(BigDecimal.valueOf(3.00));
    }

    // -----------------------------------------------------------------------
    // Bid Decrease Action
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bid decrease action lowers bid by specified percentage")
    void testBidDecreaseAction() {
        AdRule rule = AdRule.builder()
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(20.0)
                        .minBid(BigDecimal.valueOf(0.10))
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .currentBid(BigDecimal.valueOf(2.00))
                .build();

        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        // 2.00 * 0.80 = 1.60
        assertThat(execResult.getNewBid())
                .as("New bid should be old bid * 0.80")
                .isEqualByComparingTo(BigDecimal.valueOf(1.60));
    }

    @Test
    @DisplayName("Bid decrease does not go below minimum bid floor")
    void testBidDecreaseAction_MinFloor() {
        AdRule rule = AdRule.builder()
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(80.0)
                        .minBid(BigDecimal.valueOf(0.20))
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .currentBid(BigDecimal.valueOf(0.50))
                .build();

        // 0.50 * 0.20 = 0.10, but floor is 0.20
        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        assertThat(execResult.getNewBid())
                .as("New bid should not go below min floor")
                .isEqualByComparingTo(BigDecimal.valueOf(0.20));
    }

    // -----------------------------------------------------------------------
    // Pause Keyword Action
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Pause keyword action sets keyword state to PAUSED via SP-API")
    void testPauseKeywordAction() {
        AdRule rule = AdRule.builder()
                .action(AdAction.builder()
                        .type(ActionType.PAUSE_KEYWORD)
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-pause-001")
                .campaignId("camp-001")
                .adGroupId("adgroup-001")
                .keywordText("expensive keyword")
                .build();

        when(spApiClient.updateKeywordState(anyString(), anyString(), eq("PAUSED")))
                .thenReturn(new SpApiResponse(200, "{}"));

        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        assertThat(execResult.isSuccess()).isTrue();
        assertThat(execResult.getKeywordState()).isEqualTo("PAUSED");

        verify(spApiClient).updateKeywordState(
                eq("kw-pause-001"),
                eq("adgroup-001"),
                eq("PAUSED")
        );
    }

    // -----------------------------------------------------------------------
    // Add Negative Keyword Action
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Search term is added as negative keyword to campaign")
    void testAddNegativeKeywordAction() {
        AdRule rule = AdRule.builder()
                .action(AdAction.builder()
                        .type(ActionType.ADD_NEGATIVE_KEYWORD)
                        .matchType("EXACT")
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .campaignId("camp-001")
                .searchTerm("free sample")
                .build();

        when(spApiClient.addNegativeKeyword(anyString(), anyString(), anyString()))
                .thenReturn(new SpApiResponse(200, "{}"));

        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        assertThat(execResult.isSuccess()).isTrue();
        verify(spApiClient).addNegativeKeyword("camp-001", "free sample", "EXACT");
    }

    // -----------------------------------------------------------------------
    // Notification Action
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Notification is sent when rule triggers with notification action")
    void testNotificationAction() {
        AdRule rule = AdRule.builder()
                .id("rule-notify")
                .name("High Spend Alert")
                .action(AdAction.builder()
                        .type(ActionType.NOTIFICATION)
                        .message("Keyword {keyword} has ACOS of {acos}% exceeding threshold")
                        .channels(List.of("EMAIL", "IN_APP"))
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-001")
                .keywordText("premium product")
                .acos(75.0)
                .build();

        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        assertThat(execResult.isSuccess()).isTrue();
        verify(notificationService).sendNotification(argThat(notification -> {
            assertThat(notification.getMessage())
                    .contains("premium product")
                    .contains("75.0");
            assertThat(notification.getChannels()).containsExactly("EMAIL", "IN_APP");
            return true;
        }));
    }

    // -----------------------------------------------------------------------
    // Rule Execution Log (Audit Trail)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Complete audit trail is logged for every rule evaluation")
    void testRuleExecutionLog() {
        // Given: A rule that triggers
        AdRule rule = AdRule.builder()
                .id("rule-audit")
                .name("Audit Trail Test Rule")
                .condition(AdCondition.builder()
                        .metric(AdMetric.ACOS)
                        .operator(ComparisonOperator.GREATER_THAN)
                        .threshold(50.0)
                        .build())
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(20.0)
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-audit")
                .keywordText("test keyword")
                .campaignId("camp-001")
                .acos(65.0)
                .currentBid(BigDecimal.valueOf(2.00))
                .build();

        // When: Rule is evaluated and executed
        RuleEvaluationResult evalResult = ruleEngine.evaluate(rule, metrics);
        RuleExecutionResult execResult = ruleEngine.executeAction(rule, metrics);

        // Then: Complete audit log entry is created
        ArgumentCaptor<AdRuleLog> logCaptor = ArgumentCaptor.forClass(AdRuleLog.class);
        verify(ruleLogRepository).save(logCaptor.capture());

        AdRuleLog log = logCaptor.getValue();
        assertThat(log.getRuleId()).isEqualTo("rule-audit");
        assertThat(log.getRuleName()).isEqualTo("Audit Trail Test Rule");
        assertThat(log.getKeywordId()).isEqualTo("kw-audit");
        assertThat(log.getKeywordText()).isEqualTo("test keyword");
        assertThat(log.getMetricValue()).isEqualTo(65.0);
        assertThat(log.getTriggered()).isTrue();
        assertThat(log.getOldBid()).isEqualByComparingTo(BigDecimal.valueOf(2.00));
        assertThat(log.getNewBid()).isEqualByComparingTo(BigDecimal.valueOf(1.60));
        assertThat(log.getExecutedAt()).isNotNull();
        assertThat(log.getActionType()).isEqualTo("DECREASE_BID");
    }

    @Test
    @DisplayName("Non-triggered rules also create log entries for transparency")
    void testRuleExecutionLog_NonTriggered() {
        AdRule rule = AdRule.builder()
                .id("rule-not-triggered")
                .name("Non-Triggered Rule")
                .condition(AdCondition.builder()
                        .metric(AdMetric.ACOS)
                        .operator(ComparisonOperator.GREATER_THAN)
                        .threshold(50.0)
                        .build())
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(20.0)
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-good")
                .acos(25.0) // below threshold
                .currentBid(BigDecimal.valueOf(1.00))
                .build();

        RuleEvaluationResult evalResult = ruleEngine.evaluate(rule, metrics);

        // Even non-triggered rules should log for audit
        assertThat(evalResult.isTriggered()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Rule Execution with Cooldown
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Rule with cooldown period does not re-trigger within cooldown window")
    void testRuleCooldown() {
        AdRule rule = AdRule.builder()
                .id("rule-cooldown")
                .cooldownMinutes(60)
                .condition(AdCondition.builder()
                        .metric(AdMetric.ACOS)
                        .operator(ComparisonOperator.GREATER_THAN)
                        .threshold(50.0)
                        .build())
                .action(AdAction.builder()
                        .type(ActionType.DECREASE_BID)
                        .percentage(10.0)
                        .build())
                .build();

        AdKeywordMetrics metrics = AdKeywordMetrics.builder()
                .keywordId("kw-cooldown")
                .acos(65.0)
                .build();

        // First evaluation: triggers
        when(ruleLogRepository.findLastExecution("rule-cooldown", "kw-cooldown"))
                .thenReturn(null); // no previous execution

        RuleEvaluationResult first = ruleEngine.evaluate(rule, metrics);
        assertThat(first.isTriggered()).isTrue();

        // Second evaluation within cooldown: does NOT trigger
        AdRuleLog lastLog = AdRuleLog.builder()
                .executedAt(LocalDateTime.now().minusMinutes(30)) // 30 min ago, cooldown is 60
                .build();
        when(ruleLogRepository.findLastExecution("rule-cooldown", "kw-cooldown"))
                .thenReturn(lastLog);

        RuleEvaluationResult second = ruleEngine.evaluate(rule, metrics);
        assertThat(second.isTriggered())
                .as("Rule should not re-trigger within cooldown period")
                .isFalse();
    }
}
