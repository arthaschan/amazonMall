package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Function tool: Retrieve advertising campaign performance data.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetAdPerformanceFunction implements Function<GetAdPerformanceFunction.Request, GetAdPerformanceFunction.Response> {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String dateRange;
        private String campaignName;
        private Boolean includeSearchTerms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignSummary {
        private String campaignName;
        private String campaignType;
        private Double spend;
        private Double revenue;
        private Double acos;
        private Long impressions;
        private Long clicks;
        private Double ctr;
        private Double cpc;
        private Double conversionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String dateRange;
        private Double totalSpend;
        private Double totalRevenue;
        private Double overallAcos;
        private Double overallTacos;
        private List<CampaignSummary> campaigns;
        private List<String> topSearchTerms;
        private String insights;
    }

    @Override
    public Response apply(Request request) {
        log.info("getAdPerformance called: dateRange={}, campaign={}, includeSearchTerms={}",
                request.getDateRange(), request.getCampaignName(), request.getIncludeSearchTerms());

        // TODO: Inject and query the actual ad service:
        //   adService.getCampaignPerformance(tenantId, dateRange, campaignName)

        List<CampaignSummary> campaigns = List.of(
                CampaignSummary.builder()
                        .campaignName("SP - Exact Match - Core Keywords")
                        .campaignType("Sponsored Products")
                        .spend(1200.0).revenue(6800.0).acos(17.6)
                        .impressions(45000L).clicks(1800L).ctr(4.0).cpc(0.67).conversionRate(14.2)
                        .build(),
                CampaignSummary.builder()
                        .campaignName("SP - Broad Match - Discovery")
                        .campaignType("Sponsored Products")
                        .spend(800.0).revenue(2400.0).acos(33.3)
                        .impressions(62000L).clicks(2100L).ctr(3.4).cpc(0.38).conversionRate(8.5)
                        .build(),
                CampaignSummary.builder()
                        .campaignName("SB - Brand Awareness")
                        .campaignType("Sponsored Brands")
                        .spend(450.0).revenue(1800.0).acos(25.0)
                        .impressions(28000L).clicks(850L).ctr(3.0).cpc(0.53).conversionRate(10.1)
                        .build()
        );

        List<String> searchTerms = List.of();
        if (Boolean.TRUE.equals(request.getIncludeSearchTerms())) {
            searchTerms = List.of(
                    "wireless earbuds (450 clicks, 12.5% CVR)",
                    "bluetooth headphones (280 clicks, 9.8% CVR)",
                    "noise cancelling earbuds (190 clicks, 15.2% CVR)"
            );
        }

        return Response.builder()
                .dateRange(request.getDateRange() != null ? request.getDateRange() : "Last 30 days")
                .totalSpend(2450.0)
                .totalRevenue(11000.0)
                .overallAcos(22.3)
                .overallTacos(8.8)
                .campaigns(campaigns)
                .topSearchTerms(searchTerms)
                .insights("Overall ACOS 22.3% is within target. Broad match campaign has higher ACOS (33.3%) — "
                        + "consider tightening match types or adding negative keywords. "
                        + "Exact match campaign performing excellently at 17.6% ACOS.")
                .build();
    }
}
