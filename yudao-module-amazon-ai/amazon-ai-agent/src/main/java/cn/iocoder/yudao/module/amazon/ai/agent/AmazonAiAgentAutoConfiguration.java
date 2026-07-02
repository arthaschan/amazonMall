package cn.iocoder.yudao.module.amazon.ai.agent;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon AI Agent - chat assistant with Function Calling
 * Auto-configuration for this module.
 *
 * <p>Scans both the agent package (OpsAgent) and the functions sub-package
 * (all function tools like GetSalesSummaryFunction, etc.).
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
@ComponentScan(basePackageClasses = AmazonAiAgentAutoConfiguration.class)
public class AmazonAiAgentAutoConfiguration {

}
