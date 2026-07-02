package cn.iocoder.yudao.module.amazon.common.sync;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Amazon Common Sync 自动配置。
 *
 * <p>本模块提供同步框架基础设施：
 * <ul>
 *   <li>{@code AbstractAmazonSyncJob} — 同步任务抽象基类</li>
 *   <li>{@code SyncJobParam} — 任务参数 DTO</li>
 * </ul>
 *
 * <p>具体的同步 Job（如 OrderSyncJob、InventorySyncJob 等）位于各自业务模块中，
 * 通过 {@code @Component} 注册为 Spring Bean，由 Yudao 定时任务框架调度执行。
 *
 * @author AmazonOps AI
 */
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
public class AmazonCommonSyncAutoConfiguration {

}
