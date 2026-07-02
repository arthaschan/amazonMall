package cn.iocoder.yudao.module.amazon.ai.research;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon AI Research - product blueprint and recommendation engine
 * Auto-configuration for this module.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ComponentScan(basePackageClasses = AmazonAiResearchAutoConfiguration.class)
public class AmazonAiResearchAutoConfiguration {

}
