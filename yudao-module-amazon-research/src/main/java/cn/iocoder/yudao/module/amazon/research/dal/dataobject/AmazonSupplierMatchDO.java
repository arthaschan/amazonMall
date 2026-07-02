package cn.iocoder.yudao.module.amazon.research.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 供应商匹配数据对象。
 * <p>关联产品机会，记录从 1688/Alibaba 等渠道匹配到的供应商信息。</p>
 *
 * @author AmazonOps AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("amazon_supplier_match")
public class AmazonSupplierMatchDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联产品机会 ID */
    private Long opportunityId;

    /** 供应商名称 */
    private String supplierName;

    /** 供应商链接 */
    private String supplierUrl;

    /** 最小起订量 */
    private Integer minOrderQty;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 供应商评分 */
    private BigDecimal rating;

    /** 所在地 */
    private String location;

    /** 工厂类型：FACTORY / TRADING_COMPANY */
    private String factoryType;
}
