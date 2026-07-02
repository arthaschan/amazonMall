package cn.iocoder.yudao.module.amazon.shop.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Management Console - Amazon API Statistics Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmazonApiStatsRespVO {

    @Schema(description = "Per-endpoint statistics")
    private List<EndpointStats> endpoints;

    @Schema(description = "Total API calls in the period", example = "15234")
    private Long totalCalls;

    @Schema(description = "Total errors in the period", example = "42")
    private Long totalErrors;

    @Schema(description = "Overall error rate (0.0 to 1.0)", example = "0.0028")
    private Double overallErrorRate;

    @Schema(description = "Average response time in milliseconds", example = "450.5")
    private Double overallAvgResponseTimeMs;

    @Schema(description = "Per-endpoint API statistics")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointStats {

        @Schema(description = "API endpoint path", example = "/orders/v0/orders")
        private String endpoint;

        @Schema(description = "Total call count for this endpoint", example = "5230")
        private Long totalCount;

        @Schema(description = "Error count (HTTP >= 400)", example = "12")
        private Long errorCount;

        @Schema(description = "Error rate for this endpoint (0.0 to 1.0)", example = "0.0023")
        private Double errorRate;

        @Schema(description = "Average response time in ms", example = "380.5")
        private Double avgResponseTimeMs;
    }
}
