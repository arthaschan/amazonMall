package cn.iocoder.yudao.module.amazon.ai.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Amazon AI Core auto-configuration.
 *
 * <p>Registers core AI infrastructure beans:
 * <ul>
 *   <li>{@link AiProperties} - configuration binding</li>
 *   <li>{@link AiModelRouter} - model routing</li>
 *   <li>{@link PromptTemplateEngine} - template management</li>
 *   <li>{@link AiTokenTracker} - token usage tracking</li>
 *   <li>{@link AiResponseParser} - LLM response parsing</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@AutoConfiguration
@EnableConfigurationProperties(AiProperties.class)
@ComponentScan(basePackageClasses = AmazonAiCoreAutoConfiguration.class)
public class AmazonAiCoreAutoConfiguration {

    /**
     * Shared ObjectMapper for AI response parsing.
     * Configured for lenient deserialization (tolerant of LLM quirks).
     */
    @Bean("aiObjectMapper")
    public ObjectMapper aiObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
