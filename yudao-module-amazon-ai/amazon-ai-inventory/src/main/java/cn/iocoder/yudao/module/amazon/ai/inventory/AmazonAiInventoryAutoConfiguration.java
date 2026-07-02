package cn.iocoder.yudao.module.amazon.ai.inventory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon AI Inventory - demand forecasting and reorder optimization
 * Auto-configuration for this module.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ComponentScan(basePackageClasses = AmazonAiInventoryAutoConfiguration.class)
public class AmazonAiInventoryAutoConfiguration {

}
