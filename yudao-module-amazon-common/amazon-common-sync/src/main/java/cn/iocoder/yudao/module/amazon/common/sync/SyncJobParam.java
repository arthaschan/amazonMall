package cn.iocoder.yudao.module.amazon.common.sync;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步任务参数 DTO。
 *
 * <p>用于解析定时同步任务的 JSON 参数，支持按店铺、站点、时间范围过滤。
 *
 * @author AmazonOps AI
 */
@Data
public class SyncJobParam {

    /**
     * 可选，指定仅同步该店铺 ID。
     */
    private Long shopId;

    /**
     * 可选，按亚马逊站点 ID 过滤（如 ATVPDKIKX0DER）。
     */
    private String marketplaceId;

    /**
     * 可选，增量同步的起始时间；仅同步该时间之后变更的数据。
     */
    private LocalDateTime lastSyncTime;
}
