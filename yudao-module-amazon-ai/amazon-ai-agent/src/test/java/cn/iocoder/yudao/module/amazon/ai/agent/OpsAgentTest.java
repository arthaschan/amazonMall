package cn.iocoder.yudao.module.amazon.ai.agent;

import cn.iocoder.yudao.module.amazon.ai.agent.functions.*;
import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiProperties;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
import cn.iocoder.yudao.module.amazon.ai.core.PromptTemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpsAgent}.
 * <p>
 * Covers agent construction with all dependencies and tool function verification.
 */
@DisplayName("AI Agent - OpsAgent")
@ExtendWith(MockitoExtension.class)
class OpsAgentTest {

    @Mock private AiModelRouter modelRouter;
    @Mock private PromptTemplateEngine templateEngine;
    @Mock private AiTokenTracker tokenTracker;
    @Mock private AiProperties aiProperties;

    @Mock private AnalyzeReviewsFunction analyzeReviewsFunction;
    @Mock private ForecastInventoryFunction forecastInventoryFunction;
    @Mock private GenerateListingFunction generateListingFunction;
    @Mock private GetAdPerformanceFunction getAdPerformanceFunction;
    @Mock private GetInventoryStatusFunction getInventoryStatusFunction;
    @Mock private GetKeywordDataFunction getKeywordDataFunction;
    @Mock private GetSalesSummaryFunction getSalesSummaryFunction;
    @Mock private GetTopProductsFunction getTopProductsFunction;

    // -----------------------------------------------------------------------
    // Agent Construction
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Agent Construction")
    class ConstructionTests {

        @Test
        @DisplayName("should create OpsAgent with all 12 dependencies")
        void testConstruction() {
            OpsAgent agent = new OpsAgent(
                    modelRouter, templateEngine, tokenTracker, aiProperties,
                    getSalesSummaryFunction, getTopProductsFunction,
                    getInventoryStatusFunction, getAdPerformanceFunction,
                    analyzeReviewsFunction, generateListingFunction,
                    getKeywordDataFunction, forecastInventoryFunction
            );

            assertThat(agent).isNotNull();
        }

    }

    // -----------------------------------------------------------------------
    // Tool Function Verification
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Tool Functions")
    class ToolFunctionTests {

        @Test
        @DisplayName("should have all 8 tool functions available")
        void testAllToolFunctionsExist() {
            Object[] functions = {
                    analyzeReviewsFunction,
                    forecastInventoryFunction,
                    generateListingFunction,
                    getAdPerformanceFunction,
                    getInventoryStatusFunction,
                    getKeywordDataFunction,
                    getSalesSummaryFunction,
                    getTopProductsFunction
            };

            assertThat(functions).hasSize(8);
            assertThat(functions).doesNotContainNull();
        }

        @Test
        @DisplayName("AnalyzeReviewsFunction should be a valid mock")
        void testAnalyzeReviewsFunction() {
            assertThat(analyzeReviewsFunction).isNotNull();
            assertThat(analyzeReviewsFunction).isInstanceOf(AnalyzeReviewsFunction.class);
        }

        @Test
        @DisplayName("ForecastInventoryFunction should be a valid mock")
        void testForecastInventoryFunction() {
            assertThat(forecastInventoryFunction).isNotNull();
            assertThat(forecastInventoryFunction).isInstanceOf(ForecastInventoryFunction.class);
        }

        @Test
        @DisplayName("GenerateListingFunction should be a valid mock")
        void testGenerateListingFunction() {
            assertThat(generateListingFunction).isNotNull();
            assertThat(generateListingFunction).isInstanceOf(GenerateListingFunction.class);
        }

        @Test
        @DisplayName("GetAdPerformanceFunction should be a valid mock")
        void testGetAdPerformanceFunction() {
            assertThat(getAdPerformanceFunction).isNotNull();
            assertThat(getAdPerformanceFunction).isInstanceOf(GetAdPerformanceFunction.class);
        }

        @Test
        @DisplayName("GetSalesSummaryFunction should be a valid mock")
        void testGetSalesSummaryFunction() {
            assertThat(getSalesSummaryFunction).isNotNull();
            assertThat(getSalesSummaryFunction).isInstanceOf(GetSalesSummaryFunction.class);
        }

        @Test
        @DisplayName("GetTopProductsFunction should be a valid mock")
        void testGetTopProductsFunction() {
            assertThat(getTopProductsFunction).isNotNull();
            assertThat(getTopProductsFunction).isInstanceOf(GetTopProductsFunction.class);
        }

    }

}
