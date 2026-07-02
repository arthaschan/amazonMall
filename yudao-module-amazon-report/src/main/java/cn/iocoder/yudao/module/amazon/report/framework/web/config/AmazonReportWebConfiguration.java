package cn.iocoder.yudao.module.amazon.report.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Report module web configuration.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
public class AmazonReportWebConfiguration {

    /**
     * Amazon Report module API group.
     */
    @Bean
    public GroupedOpenApi amazonReportGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("amazon-report");
    }

}
