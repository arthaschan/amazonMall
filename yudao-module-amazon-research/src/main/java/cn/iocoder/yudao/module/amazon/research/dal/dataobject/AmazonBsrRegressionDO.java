package cn.iocoder.yudao.module.amazon.research.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BSR-销量回归模型参数。
 * <p>按品类 + 站点维护幂律回归系数：sales = a * bsr^alpha</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_bsr_regression")
public class AmazonBsrRegressionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类目 ID */
    private String categoryId;

    /** 站点 ID */
    private String marketplaceId;

    /** 回归系数 a */
    private BigDecimal coefficientA;

    /** 回归指数 alpha */
    private BigDecimal coefficientAlpha;

    /** R-squared 拟合优度 */
    private BigDecimal rSquared;

    /** 样本量 */
    private Integer sampleSize;

    /** 最后拟合日期 */
    private LocalDateTime lastFitDate;
}
