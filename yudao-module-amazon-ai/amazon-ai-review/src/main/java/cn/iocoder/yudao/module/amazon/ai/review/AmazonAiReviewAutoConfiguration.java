package cn.iocoder.yudao.module.amazon.ai.review;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon AI Review - review analysis and sentiment AI
 * Auto-configuration for this module.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ComponentScan(basePackageClasses = AmazonAiReviewAutoConfiguration.class)
public class AmazonAiReviewAutoConfiguration {

}
