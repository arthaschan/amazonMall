package cn.iocoder.yudao.module.amazon.ai.listing;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon AI Listing - listing generation and diagnosis AI
 * Auto-configuration for this module.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ComponentScan(basePackageClasses = AmazonAiListingAutoConfiguration.class)
public class AmazonAiListingAutoConfiguration {

}
