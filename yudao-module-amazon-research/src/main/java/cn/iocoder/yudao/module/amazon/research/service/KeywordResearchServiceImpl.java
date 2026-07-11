package cn.iocoder.yudao.module.amazon.research.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 关键词调研服务实现。
 * <p>通过 Amazon Autocomplete API 获取关键词建议和搜索量估算。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class KeywordResearchServiceImpl implements KeywordResearchService {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Long getSearchVolume(String marketplaceId, String keyword) {
        List<String> suggestions = getRelatedKeywords(marketplaceId, keyword);
        if (suggestions.isEmpty()) {
            return 0L;
        }
        // 粗略估算: 每条 autocomplete 建议约代表 1000 次月搜索量
        long estimate = suggestions.size() * 1000L;
        log.info("关键词 [{}] 搜索量估算值: {} (基于 {} 条 autocomplete 建议, 此为粗略估算)",
                keyword, estimate, suggestions.size());
        return estimate;
    }

    @Override
    public List<String> getRelatedKeywords(String marketplaceId, String seedKeyword) {
        try {
            String domain = getAmazonDomain(marketplaceId);
            String mid = getMid(marketplaceId);
            String encodedKeyword = URLEncoder.encode(seedKeyword, "UTF-8");
            String url = "https://" + domain + "/api/2017/suggestions?mid=" + mid
                    + "&alias=aps&prefix=" + encodedKeyword;

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                    .header("Accept", "application/json")
                    .get()
                    .build();

            Response response = HTTP_CLIENT.newCall(request).execute();
            try {
                if (!response.isSuccessful()) {
                    log.warn("Amazon Autocomplete 请求失败, HTTP状态码: {}, 关键词: {}",
                            response.code(), seedKeyword);
                    return Collections.emptyList();
                }

                ResponseBody body = response.body();
                if (body == null) {
                    log.warn("Amazon Autocomplete 响应体为空, 关键词: {}", seedKeyword);
                    return Collections.emptyList();
                }

                String json = body.string();
                JsonNode root = OBJECT_MAPPER.readTree(json);
                JsonNode suggestions = root.get("suggestions");
                if (suggestions == null || !suggestions.isArray()) {
                    log.warn("Amazon Autocomplete 响应中无 suggestions 字段, 关键词: {}", seedKeyword);
                    return Collections.emptyList();
                }

                List<String> keywords = new ArrayList<>();
                int maxResults = Math.min(suggestions.size(), 10);
                for (int i = 0; i < maxResults; i++) {
                    JsonNode suggestion = suggestions.get(i);
                    JsonNode valueNode = suggestion.get("value");
                    if (valueNode != null && !valueNode.isNull()) {
                        String value = valueNode.asText();
                        if (!value.isEmpty()) {
                            keywords.add(value);
                        }
                    }
                }

                log.debug("关键词 [{}] 获取到 {} 条 autocomplete 建议", seedKeyword, keywords.size());
                return keywords;
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.warn("获取 Amazon 关键词建议异常, marketplaceId: {}, seedKeyword: {}",
                    marketplaceId, seedKeyword, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据 marketplaceId 获取 Amazon Autocomplete API 域名。
     */
    private String getAmazonDomain(String marketplaceId) {
        if (marketplaceId == null) {
            return "completion.amazon.com";
        }
        switch (marketplaceId) {
            case "ATVPDKIKX0DER": return "completion.amazon.com";      // US
            case "A1F83G8C2ARO7P": return "completion.amazon.co.uk";   // UK
            case "A1PA6795UKMFR9": return "completion.amazon.de";       // DE
            case "A1VC38T7YXB528": return "completion.amazon.co.jp";   // JP
            case "A13V1IB3VIYZZH": return "completion.amazon.fr";       // FR
            case "APJ6JRA9NG5V4":  return "completion.amazon.it";       // IT
            case "A1RKKUPIHCS9HS": return "completion.amazon.es";       // ES
            case "A2EUQ1WTGCTBG2": return "completion.amazon.ca";       // CA
            case "A39IBJ37TR1P26": return "completion.amazon.com.au";   // AU
            case "A2Q3Y263D00KWC": return "completion.amazon.com.br";   // BR
            default: return "completion.amazon.com";
        }
    }

    /**
     * 根据 marketplaceId 获取 Amazon marketplace mid 参数。
     */
    private String getMid(String marketplaceId) {
        if (marketplaceId == null) {
            return "ATVPDKIKX0DER";
        }
        switch (marketplaceId) {
            case "ATVPDKIKX0DER": return "ATVPDKIKX0DER";   // US
            case "A1F83G8C2ARO7P": return "A1F83G8C2ARO7P";  // UK
            case "A1PA6795UKMFR9": return "A1PA6795UKMFR9";  // DE
            case "A1VC38T7YXB528": return "A1VC38T7YXB528";  // JP
            case "A13V1IB3VIYZZH": return "A13V1IB3VIYZZH";  // FR
            case "APJ6JRA9NG5V4":  return "APJ6JRA9NG5V4";   // IT
            case "A1RKKUPIHCS9HS": return "A1RKKUPIHCS9HS";  // ES
            case "A2EUQ1WTGCTBG2": return "A2EUQ1WTGCTBG2";  // CA
            case "A39IBJ37TR1P26": return "A39IBJ37TR1P26";  // AU
            case "A2Q3Y263D00KWC": return "A2Q3Y263D00KWC";  // BR
            default: return "ATVPDKIKX0DER";
        }
    }
}
