package cn.iocoder.yudao.module.amazon.listing.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Listing module web configuration.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
public class AmazonListingWebConfiguration {

    /**
     * Amazon Listing module API group.
     */
    @Bean
    public GroupedOpenApi amazonListingGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("amazon-listing");
    }

}
