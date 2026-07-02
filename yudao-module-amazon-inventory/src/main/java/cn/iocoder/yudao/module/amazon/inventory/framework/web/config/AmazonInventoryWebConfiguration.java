package cn.iocoder.yudao.module.amazon.inventory.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Inventory module web configuration.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
public class AmazonInventoryWebConfiguration {

    /**
     * Amazon Inventory module API group.
     */
    @Bean
    public GroupedOpenApi amazonInventoryGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("amazon-inventory");
    }

}
