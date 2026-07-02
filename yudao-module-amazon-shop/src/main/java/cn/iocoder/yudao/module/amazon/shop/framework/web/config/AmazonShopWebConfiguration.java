package cn.iocoder.yudao.module.amazon.shop.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Shop module web configuration.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
public class AmazonShopWebConfiguration {

    /**
     * Amazon Shop module API group.
     */
    @Bean
    public GroupedOpenApi amazonShopGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("amazon-shop");
    }

}
