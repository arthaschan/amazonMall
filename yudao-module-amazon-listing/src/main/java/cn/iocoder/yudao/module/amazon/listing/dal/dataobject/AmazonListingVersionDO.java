package cn.iocoder.yudao.module.amazon.listing.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Listing 版本记录。
 * <p>每次 Listing 修改都生成一个版本快照，支持回溯对比。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "amazon_listing_version", autoResultMap = true)
public class AmazonListingVersionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private Integer versionNum;
    private String title;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> bulletPoints;

    private String description;
    private String backendKeywords;

    /** 是否 AI 生成 */
    private Boolean aiGenerated;
    private BigDecimal aiScore;
    private String changeSummary;
    private Long operatorId;
}
