package cn.iocoder.yudao.module.amazon.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Amazon marketplace enumeration.
 *
 * <p>Contains marketplace IDs, country codes, regions, currencies,
 * and SP-API endpoint URLs for all supported Amazon marketplaces.
 *
 * <p>Grouped by region:
 * <ul>
 *   <li><strong>NA</strong> - North America (US, CA, MX)</li>
 *   <li><strong>EU</strong> - Europe (UK, DE, FR, IT, ES, NL, SE, PL, BE, TR, EG, AE, SA)</li>
 *   <li><strong>FE</strong> - Far East (JP, AU, IN, SG)</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@Getter
@AllArgsConstructor
public enum AmazonMarketplaceEnum {

    // ── North America ──────────────────────────────────────────────────────
    US("ATVPDKIKX0DER",  "NA", "US", "Amazon.com",      "USD", "https://sellingpartnerapi-na.amazon.com"),
    CA("A2IR4J4P5XJMGM", "NA", "CA", "Amazon.ca",       "CAD", "https://sellingpartnerapi-na.amazon.com"),
    MX("A1AM78C64UM0Y8", "NA", "MX", "Amazon.com.mx",   "MXN", "https://sellingpartnerapi-na.amazon.com"),

    // ── Europe ─────────────────────────────────────────────────────────────
    DE("A1PA6795UKMFR9", "EU", "DE", "Amazon.de",       "EUR", "https://sellingpartnerapi-eu.amazon.com"),
    ES("A1RKKUPIHCS9HS", "EU", "ES", "Amazon.es",       "EUR", "https://sellingpartnerapi-eu.amazon.com"),
    FR("A13V1IB3VIYZZH", "EU", "FR", "Amazon.fr",       "EUR", "https://sellingpartnerapi-eu.amazon.com"),
    IT("APJ6JRA9NG5V4",  "EU", "IT", "Amazon.it",       "EUR", "https://sellingpartnerapi-eu.amazon.com"),
    UK("A1F83G8C2ARO7P", "EU", "UK", "Amazon.co.uk",    "GBP", "https://sellingpartnerapi-eu.amazon.com"),
    NL("A1805IZSGTT6HS", "EU", "NL", "Amazon.nl",       "EUR", "https://sellingpartnerapi-eu.amazon.com"),
    SE("A2NODRKZP88ZB9", "EU", "SE", "Amazon.se",       "SEK", "https://sellingpartnerapi-eu.amazon.com"),
    PL("A1C3SOZRARQ6R3", "EU", "PL", "Amazon.pl",       "PLN", "https://sellingpartnerapi-eu.amazon.com"),
    BE("AMEN7S2L3V6F",   "EU", "BE", "Amazon.com.be",   "EUR", "https://sellingpartnerapi-eu.amazon.com"),
    TR("A2Q3Y263D00KWC", "EU", "TR", "Amazon.com.tr",   "TRY", "https://sellingpartnerapi-eu.amazon.com"),
    EG("ARBP9OOSHTCHU",  "EU", "EG", "Amazon.eg",       "EGP", "https://sellingpartnerapi-eu.amazon.com"),
    AE("A2VIGQ357CS4UG", "EU", "AE", "Amazon.ae",       "AED", "https://sellingpartnerapi-eu.amazon.com"),
    SA("A17E79C6D8DWNP", "EU", "SA", "Amazon.sa",       "SAR", "https://sellingpartnerapi-eu.amazon.com"),

    // ── Far East ───────────────────────────────────────────────────────────
    JP("A1VC38T7YXB528", "FE", "JP", "Amazon.co.jp",    "JPY", "https://sellingpartnerapi-fe.amazon.com"),
    AU("A39IBJ37TRP1C6", "FE", "AU", "Amazon.com.au",   "AUD", "https://sellingpartnerapi-fe.amazon.com"),
    IN("A21TJRUUN4KGV",  "FE", "IN", "Amazon.in",       "INR", "https://sellingpartnerapi-fe.amazon.com"),
    SG("A19VAU5U5O7RUS", "FE", "SG", "Amazon.sg",       "SGD", "https://sellingpartnerapi-fe.amazon.com");

    /** Amazon marketplace ID (e.g. ATVPDKIKX0DER for US). */
    private final String marketplaceId;

    /** Region code: NA, EU, or FE. */
    private final String region;

    /** ISO 3166-1 alpha-2 country code. */
    private final String countryCode;

    /** Marketplace display name. */
    private final String displayName;

    /** ISO 4217 currency code. */
    private final String currencyCode;

    /** SP-API regional endpoint URL. */
    private final String endpointUrl;

    /**
     * Finds a marketplace enum by its marketplace ID.
     *
     * @param marketplaceId the Amazon marketplace ID
     * @return the matching enum, or {@code null} if not found
     */
    public static AmazonMarketplaceEnum ofMarketplaceId(String marketplaceId) {
        if (marketplaceId == null) {
            return null;
        }
        for (AmazonMarketplaceEnum mp : values()) {
            if (mp.marketplaceId.equals(marketplaceId)) {
                return mp;
            }
        }
        return null;
    }

    /**
     * Finds a marketplace enum by country code.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return the matching enum, or {@code null} if not found
     */
    public static AmazonMarketplaceEnum ofCountryCode(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        for (AmazonMarketplaceEnum mp : values()) {
            if (mp.countryCode.equalsIgnoreCase(countryCode)) {
                return mp;
            }
        }
        return null;
    }

    /**
     * Resolves the AWS region for SP-API signing.
     *
     * @return the AWS region string (us-east-1, eu-west-1, or us-west-2)
     */
    public String getAwsRegion() {
        switch (region) {
            case "NA": return "us-east-1";
            case "EU": return "eu-west-1";
            case "FE": return "us-west-2";
            default: throw new IllegalStateException("Unknown region: " + region);
        }
    }
}
