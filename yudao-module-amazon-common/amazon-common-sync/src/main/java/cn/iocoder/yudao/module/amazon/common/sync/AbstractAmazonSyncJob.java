package cn.iocoder.yudao.module.amazon.common.sync;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Amazon 同步任务抽象基类。
 *
 * <p>所有亚马逊数据同步定时任务均继承此类，子类只需实现
 * {@link #doSync(AmazonShopDO)} 和 {@link #getJobName()} 即可完成
 * 对全部已启用店铺的遍历同步。
 *
 * <p>execute 方法会自动遍历所有已启用（status=1）的店铺，
 * 逐个调用 {@link #doSync(AmazonShopDO)}，单个店铺异常不影响其他店铺。
 *
 * @author AmazonOps AI
 */
@Slf4j
public abstract class AbstractAmazonSyncJob implements JobHandler {

    /** 店铺状态：已启用 */
    private static final int SHOP_STATUS_ENABLED = 1;

    @Resource
    private AmazonShopService amazonShopService;

    /** Jackson ObjectMapper，用于解析任务 JSON 参数 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── 抽象方法 ──────────────────────────────────────────────────────────

    /**
     * 返回任务处理器名称（与 Spring Bean 名称一致）。
     *
     * @return 任务名称
     */
    protected abstract String getJobName();

    /**
     * 执行单个店铺的同步逻辑，由子类实现。
     *
     * @param shop 店铺信息
     * @throws Exception 同步过程中可能抛出的异常
     */
    protected abstract void doSync(AmazonShopDO shop) throws Exception;

    // ── JobHandler 实现 ───────────────────────────────────────────────────

    @Override
    public String execute(String param) {
        List<AmazonShopDO> shops = getEnabledShops();
        if (shops.isEmpty()) {
            log.warn("[{}] 没有找到已启用的店铺，跳过同步", getJobName());
            return "无已启用店铺，跳过";
        }

        int successCount = 0;
        int failCount = 0;

        for (AmazonShopDO shop : shops) {
            try {
                log.info("[{}] 开始同步店铺 [id={}, name={}, marketplace={}]",
                        getJobName(), shop.getId(), shop.getShopName(), shop.getMarketplaceId());
                doSync(shop);
                successCount++;
                log.info("[{}] 店铺同步完成 [id={}]", getJobName(), shop.getId());
            } catch (Exception e) {
                failCount++;
                log.error("[{}] 店铺同步失败 [id={}, name={}]: {}",
                        getJobName(), shop.getId(), shop.getShopName(), e.getMessage(), e);
            }
        }

        String summary = String.format("同步完成: %d 成功, %d 失败", successCount, failCount);
        log.info("[{}] {}", getJobName(), summary);
        return summary;
    }

    // ── 辅助方法 ──────────────────────────────────────────────────────────

    /**
     * 获取所有已启用（status=1）的店铺列表。
     *
     * @return 已启用店铺列表
     */
    protected List<AmazonShopDO> getEnabledShops() {
        return amazonShopService.listShops().stream()
                .filter(shop -> shop.getStatus() != null && shop.getStatus() == SHOP_STATUS_ENABLED)
                .collect(Collectors.toList());
    }

    /**
     * 解析 JSON 格式的任务参数。
     *
     * @param param JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 解析后的对象；若参数为空则返回 null
     */
    protected <T> T parseJsonParam(String param, Class<T> clazz) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(param, clazz);
        } catch (Exception e) {
            log.error("[{}] 解析任务参数失败: {}", getJobName(), param, e);
            return null;
        }
    }
}
