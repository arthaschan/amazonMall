package cn.iocoder.yudao.module.amazon.order.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Order module web configuration.
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
public class AmazonOrderWebConfiguration {

    /**
     * Amazon Order module API group.
     */
    @Bean
    public GroupedOpenApi amazonOrderGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("amazon-order");
    }

}
