package cn.iocoder.yudao.module.amazon.listing.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自动调价规则。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_auto_price_rule", autoResultMap = true)
public class AmazonAutoPriceRuleDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 规则类型：COMPETITIVE / PROFIT_BASED / BUY_BOX */
    private String ruleType;

    /** 条件 JSON */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String conditionJson;

    /** 动作 JSON */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String actionJson;

    /** 状态：0=禁用 1=启用 */
    private Integer status;
}
