package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonListingVersionMapper;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Listing 生成服务实现。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AiListingGeneratorServiceImpl implements AiListingGeneratorService {

    private static final String[] STOP_WORDS = {
            "the", "is", "a", "an", "and", "or", "for", "of", "to", "in", "with", "by",
            "this", "that", "it", "on", "at", "from"
    };

    @Resource
    private AmazonProductMapper productMapper;

    @Resource
    private AmazonListingVersionMapper versionMapper;

    @Override
    public AmazonListingVersionDO generate(Long productId, String prompt) {
        AmazonProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        // 1. Build optimized title
        String title = buildOptimizedTitle(product, prompt);

        // 2. Generate 5 bullet points
        List<String> bullets = generateBullets(product, prompt);

        // 3. Use product description or generate from prompt
        String description = product.getDescription();
        if (description == null || description.isEmpty()) {
            description = "High quality " + (product.getBrand() != null ? product.getBrand() : "")
                    + " product. " + prompt;
        }

        // 4. Extract backend keywords
        String backendKeywords = extractBackendKeywords(prompt, title, bullets);

        // 5. Calculate AI score
        BigDecimal aiScore = calculateAiScore(title, bullets, backendKeywords, description);

        // 6. Create new version
        AmazonListingVersionDO version = buildNewVersion(product, true);
        version.setTitle(title);
        version.setBulletPoints(bullets);
        version.setDescription(description);
        version.setBackendKeywords(backendKeywords);
        version.setAiGenerated(true);
        version.setAiScore(aiScore);
        version.setChangeSummary("AI 全新生成: " + prompt);
        versionMapper.insert(version);

        log.info("[AiListing] Generated new listing version {} for product [id={}], aiScore={}",
                version.getVersionNum(), productId, aiScore);
        return version;
    }

    @Override
    public AmazonListingVersionDO optimize(Long productId, String prompt) {
        AmazonProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        // 1. Get latest version or use product fields
        List<AmazonListingVersionDO> versions = versionMapper.selectByProductId(productId);
        AmazonListingVersionDO latestVersion = null;
        if (!versions.isEmpty()) {
            latestVersion = versions.get(versions.size() - 1);
        }

        String currentTitle = latestVersion != null ? latestVersion.getTitle() : product.getTitle();
        List<String> currentBullets = latestVersion != null && latestVersion.getBulletPoints() != null
                ? latestVersion.getBulletPoints() : product.getBulletPoints();
        String currentDescription = latestVersion != null ? latestVersion.getDescription() : product.getDescription();
        String currentBackendKeywords = latestVersion != null ? latestVersion.getBackendKeywords()
                : product.getBackendKeywords();

        StringBuilder changeSummaryBuilder = new StringBuilder("AI 优化: ");
        List<String> issues = new ArrayList<>();

        // 2. Analyze issues
        String newTitle = currentTitle;
        if (currentTitle == null || currentTitle.length() < 80 || currentTitle.length() > 200) {
            newTitle = buildOptimizedTitle(product, prompt);
            issues.add("title optimized (was " + (currentTitle != null ? currentTitle.length() : 0)
                    + " chars, now " + newTitle.length() + " chars)");
        }

        List<String> newBullets = currentBullets;
        if (currentBullets == null || currentBullets.size() < 5) {
            newBullets = generateBullets(product, prompt);
            issues.add("bullets regenerated (had " + (currentBullets != null ? currentBullets.size() : 0)
                    + ", now " + newBullets.size() + ")");
        }

        String newBackendKeywords = currentBackendKeywords;
        if (currentBackendKeywords == null || currentBackendKeywords.isEmpty()) {
            newBackendKeywords = extractBackendKeywords(prompt, newTitle, newBullets);
            issues.add("backend keywords generated");
        } else {
            // Merge prompt keywords into existing backend keywords
            String additionalKeywords = extractBackendKeywords(prompt, newTitle, newBullets);
            if (!additionalKeywords.isEmpty()) {
                String combined = currentBackendKeywords + " " + additionalKeywords;
                byte[] combinedBytes;
                try {
                    combinedBytes = combined.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    combinedBytes = combined.getBytes();
                }
                if (combinedBytes.length > 250) {
                    combined = truncateToUtf8Bytes(combined, 250);
                }
                newBackendKeywords = combined;
                issues.add("backend keywords enriched with prompt terms");
            }
        }

        String newDescription = currentDescription;
        if (newDescription == null || newDescription.isEmpty()) {
            newDescription = "High quality " + (product.getBrand() != null ? product.getBrand() : "")
                    + " product. " + prompt;
            issues.add("description generated");
        }

        // Apply prompt keywords to title and bullets if prompt contains specific keywords
        if (!issues.isEmpty() && prompt != null && !prompt.isEmpty()) {
            String[] promptWords = prompt.split("\\s+");
            if (promptWords.length > 2 && newTitle != null) {
                // Merge first meaningful keyword from prompt into title if not present
                String promptKeyword = findFirstMeaningfulWord(prompt);
                if (promptKeyword != null && !newTitle.toLowerCase().contains(promptKeyword.toLowerCase())) {
                    if (newTitle.length() + promptKeyword.length() + 2 <= 200) {
                        newTitle = newTitle + " - " + promptKeyword;
                    }
                }
            }
        }

        for (String issue : issues) {
            if (changeSummaryBuilder.length() > 0 && changeSummaryBuilder.charAt(changeSummaryBuilder.length() - 1) != ' ') {
                changeSummaryBuilder.append(", ");
            }
            changeSummaryBuilder.append(issue);
        }

        // 5. Calculate improved AI score
        BigDecimal aiScore = calculateAiScore(newTitle, newBullets, newBackendKeywords, newDescription);

        AmazonListingVersionDO version = buildNewVersion(product, true);
        version.setTitle(newTitle);
        version.setBulletPoints(newBullets);
        version.setDescription(newDescription);
        version.setBackendKeywords(newBackendKeywords);
        version.setAiGenerated(true);
        version.setAiScore(aiScore);
        version.setChangeSummary(changeSummaryBuilder.toString());
        versionMapper.insert(version);

        log.info("[AiListing] Optimized listing version {} for product [id={}], aiScore={}, issues={}",
                version.getVersionNum(), productId, aiScore, issues);
        return version;
    }

    // ========================= helper methods =========================

    private AmazonListingVersionDO buildNewVersion(AmazonProductDO product, boolean aiGenerated) {
        List<AmazonListingVersionDO> versions = versionMapper.selectByProductId(product.getId());
        int nextVersion = versions.isEmpty() ? 1 : versions.stream()
                .mapToInt(AmazonListingVersionDO::getVersionNum).max().orElse(0) + 1;

        AmazonListingVersionDO version = new AmazonListingVersionDO();
        version.setProductId(product.getId());
        version.setVersionNum(nextVersion);
        version.setTitle(product.getTitle());
        version.setBulletPoints(product.getBulletPoints());
        version.setDescription(product.getDescription());
        version.setBackendKeywords(product.getBackendKeywords());
        version.setAiGenerated(aiGenerated);
        return version;
    }

    /**
     * Build optimized title: "[Brand] - [Primary Keyword from prompt] [Category] - [Benefit]" (max 200 chars).
     */
    private String buildOptimizedTitle(AmazonProductDO product, String prompt) {
        String brand = product.getBrand() != null ? product.getBrand() : "";
        String category = product.getCategoryId() != null ? product.getCategoryId().toString() : "";

        // Extract primary keyword from prompt (first meaningful word)
        String primaryKey = extractPrimaryKeyword(prompt);

        // Build benefit from prompt context
        String benefit = extractBenefit(prompt);

        StringBuilder titleBuilder = new StringBuilder();
        if (!brand.isEmpty()) {
            titleBuilder.append(brand);
        }
        if (!primaryKey.isEmpty()) {
            if (titleBuilder.length() > 0) {
                titleBuilder.append(" - ");
            }
            titleBuilder.append(primaryKey);
        }
        if (!category.isEmpty()) {
            titleBuilder.append(" ").append(category);
        }
        if (!benefit.isEmpty()) {
            titleBuilder.append(" - ").append(benefit);
        }

        String title = titleBuilder.toString();
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }
        return title;
    }

    /**
     * Extract the primary keyword from the prompt (first non-stop word longer than 2 chars).
     */
    private String extractPrimaryKeyword(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return "";
        }
        String[] words = prompt.split("\\s+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (cleaned.length() > 2 && !isStopWord(cleaned)) {
                return capitalizeFirst(word.replaceAll("[^a-zA-Z0-9\\s-]", ""));
            }
        }
        return "";
    }

    /**
     * Extract a benefit phrase from the prompt.
     */
    private String extractBenefit(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return "Premium Quality Product";
        }
        String lower = prompt.toLowerCase();
        if (lower.contains("durable") || lower.contains("strong")) {
            return "Durable and Long Lasting";
        }
        if (lower.contains("lightweight") || lower.contains("portable")) {
            return "Lightweight and Portable";
        }
        if (lower.contains("waterproof") || lower.contains("water resistant")) {
            return "Waterproof Protection";
        }
        if (lower.contains("comfortable") || lower.contains("soft")) {
            return "Superior Comfort";
        }
        if (lower.contains("fast") || lower.contains("quick")) {
            return "Fast and Efficient";
        }
        if (lower.contains("eco") || lower.contains("green") || lower.contains("sustainable")) {
            return "Eco Friendly Design";
        }
        return "Premium Quality Product";
    }

    /**
     * Generate 5 bullet points. If product has existing bulletPoints, enhance them;
     * otherwise create from description + prompt keywords.
     * Each bullet starts with UPPERCASE benefit phrase, 150-250 chars each.
     */
    private List<String> generateBullets(AmazonProductDO product, String prompt) {
        List<String> bullets = new ArrayList<>();
        String promptLower = prompt != null ? prompt.toLowerCase() : "";
        List<String> keywords = extractKeywordsFromPrompt(prompt);

        if (product.getBulletPoints() != null && !product.getBulletPoints().isEmpty()) {
            // Enhance existing bullet points
            List<String> existing = product.getBulletPoints();
            for (int i = 0; i < existing.size() && i < 5; i++) {
                String bullet = existing.get(i);
                String enhanced = enhanceBullet(bullet, keywords, i);
                bullets.add(enhanced);
            }
            // Fill remaining slots to reach 5
            String[] defaultBenefits = {
                    "PREMIUM QUALITY", "EASY TO USE", "PERFECT FIT", "GREAT VALUE", "SATISFACTION GUARANTEED"
            };
            for (int i = bullets.size(); i < 5; i++) {
                String keyword = i < keywords.size() ? keywords.get(i) : "product";
                String bullet = defaultBenefits[i] + " - This " + keyword
                        + " delivers exceptional performance and reliability for everyday use, "
                        + "designed to exceed your expectations with premium materials and craftsmanship.";
                bullets.add(truncateBullet(bullet));
            }
        } else {
            // Create from description + prompt keywords
            String desc = product.getDescription() != null ? product.getDescription() : "";
            String[] defaultBenefits = {
                    "PREMIUM QUALITY", "EASY TO USE", "VERSATILE DESIGN", "GREAT VALUE", "SATISFACTION GUARANTEED"
            };

            // First pass: keyword-based bullets
            for (int i = 0; i < 5 && i < keywords.size(); i++) {
                String keyword = keywords.get(i);
                String benefitPhrase = keyword.toUpperCase();
                String bullet = benefitPhrase + " - " + buildBulletContent(keyword, desc, prompt);
                bullets.add(truncateBullet(bullet));
            }

            // Fill remaining with default benefit bullets
            for (int i = bullets.size(); i < 5; i++) {
                String keyword = i < keywords.size() ? keywords.get(i) : "product";
                String bullet = defaultBenefits[i] + " - This " + keyword
                        + " is crafted with precision to provide outstanding functionality, "
                        + "making it an ideal choice for daily use and a perfect gift option.";
                bullets.add(truncateBullet(bullet));
            }
        }

        // Ensure exactly 5
        while (bullets.size() > 5) {
            bullets.remove(bullets.size() - 1);
        }

        return bullets;
    }

    /**
     * Extract meaningful keywords from the prompt (non-stop words longer than 2 chars).
     */
    private List<String> extractKeywordsFromPrompt(String prompt) {
        List<String> keywords = new ArrayList<>();
        if (prompt == null || prompt.isEmpty()) {
            return keywords;
        }
        String[] words = prompt.split("\\s+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (cleaned.length() > 2 && !isStopWord(cleaned)) {
                keywords.add(cleaned);
            }
        }
        return keywords;
    }

    private String enhanceBullet(String bullet, List<String> keywords, int index) {
        String upper = bullet.toUpperCase();
        // If bullet already starts with uppercase phrase, keep it
        if (upper.equals(bullet) || (bullet.length() > 3 && bullet.substring(0, 3).equals(bullet.substring(0, 3).toUpperCase()))) {
            // Already has uppercase prefix, just ensure length
            return truncateBullet(bullet);
        }

        String benefitPrefix;
        if (!keywords.isEmpty() && index < keywords.size()) {
            benefitPrefix = keywords.get(index).toUpperCase();
        } else {
            String[] defaults = {"PREMIUM QUALITY", "EASY TO USE", "VERSATILE DESIGN", "GREAT VALUE", "TRUSTED BRAND"};
            benefitPrefix = defaults[index % defaults.length];
        }

        String enhanced = benefitPrefix + " - " + bullet;
        return truncateBullet(enhanced);
    }

    private String buildBulletContent(String keyword, String description, String prompt) {
        StringBuilder content = new StringBuilder();
        content.append("Our ").append(keyword).append(" features advanced design and superior materials");
        if (!description.isEmpty()) {
            content.append(". ").append(description.length() > 80 ? description.substring(0, 80) : description);
        }
        content.append(". Perfect for everyday use with reliable performance and lasting durability.");
        return content.toString();
    }

    /**
     * Truncate bullet to 250 chars, ensuring minimum 150 chars by padding if needed.
     */
    private String truncateBullet(String bullet) {
        if (bullet.length() > 250) {
            bullet = bullet.substring(0, 247) + "...";
        }
        while (bullet.length() < 150) {
            bullet = bullet + " High quality guaranteed.";
        }
        if (bullet.length() > 250) {
            bullet = bullet.substring(0, 247) + "...";
        }
        return bullet;
    }

    /**
     * Extract unique words from prompt + title not in visible listing,
     * remove stop words, no duplicates, max 250 bytes UTF-8.
     */
    private String extractBackendKeywords(String prompt, String title, List<String> bullets) {
        List<String> visibleWords = new ArrayList<>();

        // Collect visible words from title
        if (title != null) {
            for (String word : title.toLowerCase().split("\\s+")) {
                String cleaned = word.replaceAll("[^a-z0-9]", "");
                if (!cleaned.isEmpty()) {
                    visibleWords.add(cleaned);
                }
            }
        }

        // Collect visible words from bullets
        if (bullets != null) {
            for (String bullet : bullets) {
                if (bullet == null) {
                    continue;
                }
                for (String word : bullet.toLowerCase().split("\\s+")) {
                    String cleaned = word.replaceAll("[^a-z0-9]", "");
                    if (!cleaned.isEmpty()) {
                        visibleWords.add(cleaned);
                    }
                }
            }
        }

        // Collect candidate words from prompt and title
        List<String> candidateWords = new ArrayList<>();
        if (prompt != null) {
            for (String word : prompt.split("\\s+")) {
                String cleaned = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleaned.length() > 1) {
                    candidateWords.add(cleaned);
                }
            }
        }
        if (title != null) {
            for (String word : title.split("\\s+")) {
                String cleaned = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleaned.length() > 1) {
                    candidateWords.add(cleaned);
                }
            }
        }

        // Filter: not stop words, not in visible words, no duplicates
        List<String> backendWords = new ArrayList<>();
        for (String word : candidateWords) {
            if (isStopWord(word)) {
                continue;
            }
            if (visibleWords.contains(word)) {
                continue;
            }
            if (backendWords.contains(word)) {
                continue;
            }
            backendWords.add(word);
        }

        // Join and ensure max 250 bytes UTF-8
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backendWords.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(backendWords.get(i));
        }

        return truncateToUtf8Bytes(sb.toString(), 250);
    }

    /**
     * Truncate string to fit within maxBytes when encoded as UTF-8.
     */
    private String truncateToUtf8Bytes(String text, int maxBytes) {
        try {
            byte[] bytes = text.getBytes("UTF-8");
            if (bytes.length <= maxBytes) {
                return text;
            }
            // Trim byte by byte
            byte[] trimmed = new byte[maxBytes];
            System.arraycopy(bytes, 0, trimmed, 0, maxBytes);
            String result = new String(trimmed, "UTF-8");
            // Remove trailing partial character
            if (result.charAt(result.length() - 1) == '\uFFFD') {
                result = result.substring(0, result.length() - 1);
            }
            // Trim trailing space
            result = result.trim();
            return result;
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported, but fallback just in case
            if (text.length() > maxBytes) {
                return text.substring(0, maxBytes);
            }
            return text;
        }
    }

    /**
     * Calculate AI score: title length (30pts), bullet count (30pts),
     * backend keywords present (20pts), has description (20pts). Total 0-100.
     */
    private BigDecimal calculateAiScore(String title, List<String> bullets,
                                        String backendKeywords, String description) {
        int score = 0;

        // Title length score: 200 chars = 30pts, proportional
        if (title != null && !title.isEmpty()) {
            int titleLen = title.length();
            int titleScore = Math.min(30, (int) ((double) titleLen / 200.0 * 30.0));
            score += titleScore;
        }

        // Bullet count score: 5 bullets = 30pts, proportional
        if (bullets != null) {
            int bulletScore = Math.min(30, bullets.size() * 6);
            score += bulletScore;
        }

        // Backend keywords present: 20pts
        if (backendKeywords != null && !backendKeywords.isEmpty()) {
            score += 20;
        }

        // Has description: 20pts
        if (description != null && !description.isEmpty()) {
            score += 20;
        }

        return new BigDecimal(score);
    }

    /**
     * Check if a word is a stop word.
     */
    private boolean isStopWord(String word) {
        if (word == null) {
            return true;
        }
        String lower = word.toLowerCase();
        for (String stopWord : STOP_WORDS) {
            if (stopWord.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private String capitalizeFirst(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private String findFirstMeaningfulWord(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] words = text.split("\\s+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (cleaned.length() > 2 && !isStopWord(cleaned)) {
                return word.replaceAll("[^a-zA-Z0-9\\s-]", "");
            }
        }
        return null;
    }
}
