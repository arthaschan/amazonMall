package cn.iocoder.yudao.module.amazon.review.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 评论预警数据对象。
 * <p>当出现差评或异常评论时触发预警。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_review_alert")
public class AmazonReviewAlertDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;
    private String asin;
    private String reviewId;

    /** 星级 */
    private Integer rating;

    /** 预警时间 */
    private LocalDateTime alertTime;

    /** 是否已处理 */
    private Boolean acknowledged;

    /** AI 分析结论 */
    private String aiAnalysis;

    /** 建议操作 */
    private String suggestedAction;
}
