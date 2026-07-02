package cn.iocoder.yudao.module.amazon.research.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 细分品类（Niche）数据对象。
 * <p>记录每个品类的十维评分及综合权重，用于选品调研。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_niche", autoResultMap = true)
public class AmazonNicheDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 品类名称 */
    private String name;

    /** 站点 ID */
    private String marketplaceId;

    /** 所属类目 */
    private String category;

    /** 状态：0=草稿，1=分析中，2=已完成，3=已归档 */
    private Integer status;

    /** 全知评分（Omniscient Score）0-100 */
    private BigDecimal omniscientScore;

    /** 需求维度评分 */
    private BigDecimal demandScore;

    /** 竞争维度评分 */
    private BigDecimal competitionScore;

    /** 盈利维度评分 */
    private BigDecimal profitabilityScore;

    /** 评论护城河评分 */
    private BigDecimal reviewMoatScore;

    /** 价格稳定性评分 */
    private BigDecimal priceStabilityScore;

    /** 季节性评分 */
    private BigDecimal seasonalityScore;

    /** 自然排名评分 */
    private BigDecimal organicRankScore;

    /** 广告依赖度评分 */
    private BigDecimal adDependencyScore;

    /** 供应商评分 */
    private BigDecimal supplierScore;

    /** 评分权重 JSON，key=维度名，value=权重 0~1 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, BigDecimal> scoreWeights;
}
