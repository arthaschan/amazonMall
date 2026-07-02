package cn.iocoder.yudao.module.amazon.ai.core.dal.mysql;

import cn.iocoder.yudao.module.amazon.ai.core.dal.dataobject.AiTokenUsageDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis mapper for AI token usage records.
 *
 * @author AmazonOps AI
 */
@Mapper
public interface AiTokenUsageMapper extends BaseMapper<AiTokenUsageDO> {

    /**
     * Sum total tokens for a given tenant and year-month.
     *
     * @param tenantId  tenant ID
     * @param yearMonth year-month string, e.g. "2025-06"
     * @return total tokens consumed, or 0 if none
     */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM amazon_ai_token_usage " +
            "WHERE tenant_id = #{tenantId} AND year_month = #{yearMonth}")
    long sumTokensByTenantAndMonth(@Param("tenantId") Long tenantId,
                                   @Param("yearMonth") String yearMonth);

    /**
     * Sum estimated cost for a given tenant and year-month.
     */
    @Select("SELECT COALESCE(SUM(estimated_cost_usd), 0.0) FROM amazon_ai_token_usage " +
            "WHERE tenant_id = #{tenantId} AND year_month = #{yearMonth}")
    double sumCostByTenantAndMonth(@Param("tenantId") Long tenantId,
                                   @Param("yearMonth") String yearMonth);

    /**
     * Query wrapper helper: find all usage records for a tenant in a given month.
     */
    default LambdaQueryWrapper<AiTokenUsageDO> queryByTenantAndMonth(Long tenantId, String yearMonth) {
        return new LambdaQueryWrapper<AiTokenUsageDO>()
                .eq(AiTokenUsageDO::getTenantId, tenantId)
                .eq(AiTokenUsageDO::getYearMonth, yearMonth)
                .orderByDesc(AiTokenUsageDO::getCreateTime);
    }
}
