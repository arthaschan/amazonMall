package cn.iocoder.yudao.module.amazon.ai.ad;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon AI Ad - advertising optimization AI
 * Auto-configuration for this module.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ComponentScan(basePackageClasses = AmazonAiAdAutoConfiguration.class)
public class AmazonAiAdAutoConfiguration {

}
