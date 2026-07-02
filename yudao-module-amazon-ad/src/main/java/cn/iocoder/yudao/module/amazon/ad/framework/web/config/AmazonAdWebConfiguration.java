package cn.iocoder.yudao.module.amazon.ad.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Ad module web configuration.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
public class AmazonAdWebConfiguration {

    /**
     * Amazon Ad module API group.
     */
    @Bean
    public GroupedOpenApi amazonAdGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("amazon-ad");
    }

}
