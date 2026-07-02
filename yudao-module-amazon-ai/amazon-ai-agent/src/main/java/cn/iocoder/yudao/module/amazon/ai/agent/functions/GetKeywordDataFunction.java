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
 * Function tool: Retrieve keyword ranking and search volume data.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetKeywordDataFunction implements Function<GetKeywordDataFunction.Request, GetKeywordDataFunction.Response> {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** The keyword to look up */
        private String keyword;
        /** Optional ASIN for rank tracking */
        private String asin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordData {
        private String keyword;
        private Long monthlySearchVolume;
        private Double searchVolumeTrend; // percentage change vs last month
        private Integer organicRank;
        private Integer sponsoredRank;
        private Double clickShare;
        private Double conversionShare;
        private String competitionLevel; // high, medium, low
        private Double suggestedBid;
        private List<String> relatedKeywords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private KeywordData keywordData;
        private List<KeywordData> relatedKeywords;
        private String insights;
    }

    @Override
    public Response apply(Request request) {
        log.info("getKeywordData called: keyword={}, asin={}", request.getKeyword(), request.getAsin());

        // TODO: Inject and query the actual keyword/research service:
        //   keywordService.getKeywordData(tenantId, keyword, asin)

        KeywordData mainKeyword = KeywordData.builder()
                .keyword(request.getKeyword() != null ? request.getKeyword() : "wireless earbuds")
                .monthlySearchVolume(245000L)
                .searchVolumeTrend(5.2)
                .organicRank(request.getAsin() != null ? 12 : null)
                .sponsoredRank(request.getAsin() != null ? 3 : null)
                .clickShare(request.getAsin() != null ? 4.8 : null)
                .conversionShare(request.getAsin() != null ? 6.2 : null)
                .competitionLevel("high")
                .suggestedBid(1.85)
                .relatedKeywords(List.of(
                        "wireless earbuds for android",
                        "bluetooth earbuds with mic",
                        "noise cancelling earbuds",
                        "earbuds for running",
                        "cheap wireless earbuds"
                ))
                .build();

        List<KeywordData> related = List.of(
                KeywordData.builder()
                        .keyword("wireless earbuds for android")
                        .monthlySearchVolume(89000L)
                        .competitionLevel("medium")
                        .suggestedBid(1.45)
                        .build(),
                KeywordData.builder()
                        .keyword("noise cancelling earbuds")
                        .monthlySearchVolume(156000L)
                        .competitionLevel("high")
                        .suggestedBid(2.20)
                        .build(),
                KeywordData.builder()
                        .keyword("earbuds for running")
                        .monthlySearchVolume(67000L)
                        .competitionLevel("medium")
                        .suggestedBid(1.30)
                        .build()
        );

        String insights = request.getAsin() != null
                ? String.format("Your product ranks #%d organically and #%d sponsored for '%s'. "
                        + "Click share is %.1f%% with %.1f%% conversion share — above average for your rank position. "
                        + "Consider increasing sponsored bids to improve visibility.",
                        mainKeyword.getOrganicRank(), mainKeyword.getSponsoredRank(),
                        mainKeyword.getKeyword(),
                        mainKeyword.getClickShare(), mainKeyword.getConversionShare())
                : String.format("'%s' has %d monthly searches with %s competition. "
                                + "Search volume trending %+.1f%%. Suggested bid: $%.2f.",
                        mainKeyword.getKeyword(), mainKeyword.getMonthlySearchVolume(),
                        mainKeyword.getCompetitionLevel(),
                        mainKeyword.getSearchVolumeTrend(), mainKeyword.getSuggestedBid());

        return Response.builder()
                .keywordData(mainKeyword)
                .relatedKeywords(related)
                .insights(insights)
                .build();
    }
}
