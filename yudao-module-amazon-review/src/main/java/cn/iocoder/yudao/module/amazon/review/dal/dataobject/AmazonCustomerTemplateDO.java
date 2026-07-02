package cn.iocoder.yudao.module.amazon.review.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客服消息模板。
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_customer_template", autoResultMap = true)
public class AmazonCustomerTemplateDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String templateName;

    /** 模板类型: REFUND / EXCHANGE / GUIDE / THANKS / APOLOGY */
    private String templateType;

    /** 语言: en / zh / de / ja ... */
    private String language;

    private String subject;
    private String body;

    /** 模板变量列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> variables;
}
