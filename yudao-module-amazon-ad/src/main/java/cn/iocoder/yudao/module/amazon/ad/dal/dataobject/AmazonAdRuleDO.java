package cn.iocoder.yudao.module.amazon.ad.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 广告自动化规则。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_ad_rule", autoResultMap = true)
public class AmazonAdRuleDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long shopId;

    private String ruleName;

    /** 作用范围：CAMPAIGN / ADGROUP / KEYWORD */
    private String scope;

    /** 条件 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String conditionJson;

    /** 动作 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String actionJson;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 最后执行时间 */
    private LocalDateTime lastExecutedAt;
}
