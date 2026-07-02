package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.module.amazon.ai.listing.AiListingGeneratorService;
import cn.iocoder.yudao.module.amazon.ai.listing.dto.ListingDtos.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Function tool: Generate an optimized product listing.
 *
 * <p>Delegates to {@link AiListingGeneratorService} for actual generation.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GenerateListingFunction implements Function<GenerateListingFunction.Request, GenerateListingFunction.Response> {

    private final AiListingGeneratorService listingGeneratorService;

    public GenerateListingFunction(AiListingGeneratorService listingGeneratorService) {
        this.listingGeneratorService = listingGeneratorService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** ASIN for existing product, or null for new product */
        private String asin;
        /** Product type/category for new products */
        private String productType;
        /** Target keywords to optimize for */
        private List<String> targetKeywords;
        /** Target language code, e.g. "en" */
        private String language;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String title;
        private List<String> bulletPoints;
        private String description;
        private String backendKeywords;
        private String validationStatus;
        private Double keywordCoveragePct;
    }

    @Override
    public Response apply(Request request) {
        log.info("generateListing called: asin={}, productType={}, keywords={}, language={}",
                request.getAsin(), request.getProductType(),
                request.getTargetKeywords(), request.getLanguage());

        // Build ProductInfo
        ProductInfo productInfo = ProductInfo.builder()
                .asin(request.getAsin())
                .productType(request.getProductType())
                .targetLanguage(request.getLanguage() != null ? request.getLanguage() : "en")
                .build();

        List<String> keywords = request.getTargetKeywords() != null
                ? request.getTargetKeywords() : List.of();

        try {
            GeneratedListing listing = listingGeneratorService.generateListing(productInfo, keywords);

            return Response.builder()
                    .title(listing.getTitle())
                    .bulletPoints(listing.getBulletPoints())
                    .description(listing.getDescription())
                    .backendKeywords(listing.getBackendKeywords())
                    .validationStatus(listing.getValidation() != null && listing.getValidation().isPassed()
                            ? "PASSED" : "NEEDS REVIEW")
                    .keywordCoveragePct(listing.getKeywordCoverage() != null
                            ? listing.getKeywordCoverage().getCoveragePercentage() : 0.0)
                    .build();
        } catch (Exception e) {
            log.error("Listing generation failed: {}", e.getMessage(), e);
            return Response.builder()
                    .title("Generation failed")
                    .bulletPoints(List.of("Error: " + e.getMessage()))
                    .validationStatus("ERROR")
                    .build();
        }
    }
}
