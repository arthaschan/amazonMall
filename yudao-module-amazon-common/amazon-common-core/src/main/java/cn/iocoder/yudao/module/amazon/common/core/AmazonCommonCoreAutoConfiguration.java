package cn.iocoder.yudao.module.amazon.common.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yudao.module.amazon.common.core.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;

/**
 * Auto-configuration for the Amazon SP-API common core module.
 *
 * <p>This configuration is activated when the {@code amazon.sp-api.*} properties
 * are present in the application configuration. It wires up the full SP-API
 * infrastructure:
 * <ul>
 *   <li>{@link AmazonProperties} -- configuration binding</li>
 *   <li>{@link AmazonCredentialEncryptor} -- AES-256-GCM encryption for stored secrets</li>
 *   <li>{@link SpApiRetryPolicy} -- exponential backoff retry logic</li>
 *   <li>{@link SpApiRateLimiter} -- distributed token-bucket rate limiter (Redis)</li>
 *   <li>{@link SpApiTokenRefresher} -- LWA OAuth2 token exchange and refresh</li>
 *   <li>{@link SpApiTokenStore} -- two-tier token management (Redis + MySQL)</li>
 *   <li>{@link SpApiClient} -- unified HTTP client with SigV4 signing</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "amazon.sp-api", name = "app-id")
@EnableConfigurationProperties(AmazonProperties.class)
public class AmazonCommonCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AmazonCredentialEncryptor amazonCredentialEncryptor(AmazonProperties properties) {
        return new AmazonCredentialEncryptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpApiRetryPolicy spApiRetryPolicy(AmazonProperties properties) {
        return new SpApiRetryPolicy(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public SpApiRateLimiter spApiRateLimiter(StringRedisTemplate redisTemplate,
                                              AmazonProperties properties) {
        return new SpApiRateLimiter(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpApiTokenRefresher spApiTokenRefresher(AmazonProperties properties,
                                                    ObjectMapper objectMapper) {
        return new SpApiTokenRefresher(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({StringRedisTemplate.class, DataSource.class})
    public SpApiTokenStore spApiTokenStore(StringRedisTemplate redisTemplate,
                                            AmazonProperties properties,
                                            AmazonCredentialEncryptor encryptor,
                                            SpApiTokenRefresher tokenRefresher,
                                            DataSource dataSource) {
        return new SpApiTokenStore(redisTemplate, properties, encryptor, tokenRefresher, dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpApiClient spApiClient(AmazonProperties properties,
                                    SpApiTokenStore tokenStore,
                                    SpApiRateLimiter rateLimiter,
                                    SpApiRetryPolicy retryPolicy,
                                    ObjectMapper objectMapper) {
        return new SpApiClient(properties, tokenStore, rateLimiter, retryPolicy, objectMapper);
    }
}
