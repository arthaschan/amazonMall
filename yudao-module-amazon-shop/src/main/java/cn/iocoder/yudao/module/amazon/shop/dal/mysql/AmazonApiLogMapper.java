package cn.iocoder.yudao.module.amazon.shop.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiLogPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonApiLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MyBatis Plus mapper for {@link AmazonApiLogDO}.
 *
 * @author AmazonOps AI
 */
@Mapper
public interface AmazonApiLogMapper extends BaseMapperX<AmazonApiLogDO> {

    /**
     * Paginated query of API logs with optional filters.
     *
     * @param reqVO the page request with filter parameters
     * @return paginated result
     */
    default PageResult<AmazonApiLogDO> selectPage(AmazonApiLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonApiLogDO>()
                .eqIfPresent(AmazonApiLogDO::getShopId, reqVO.getShopId())
                .likeIfPresent(AmazonApiLogDO::getApiEndpoint, reqVO.getApiEndpoint())
                .eqIfPresent(AmazonApiLogDO::getRequestMethod, reqVO.getRequestMethod())
                .eqIfPresent(AmazonApiLogDO::getResponseCode, reqVO.getResponseCode())
                .betweenIfPresent(AmazonApiLogDO::getCreateTime, reqVO.getCreateTimeStart(), reqVO.getCreateTimeEnd())
                .orderByDesc(AmazonApiLogDO::getId));
    }

    /**
     * Finds slow requests exceeding the given response time threshold.
     *
     * @param shopId         optional shop ID filter
     * @param thresholdMs    response time threshold in milliseconds
     * @param startTime      start of time range
     * @param endTime        end of time range
     * @return list of slow API log entries
     */
    default List<AmazonApiLogDO> selectSlowRequests(Long shopId, int thresholdMs,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<AmazonApiLogDO>()
                .eqIfPresent(AmazonApiLogDO::getShopId, shopId)
                .ge(AmazonApiLogDO::getResponseTimeMs, thresholdMs)
                .between(AmazonApiLogDO::getCreateTime, startTime, endTime)
                .orderByDesc(AmazonApiLogDO::getResponseTimeMs));
    }

    /**
     * Counts total API calls and error calls grouped by endpoint.
     *
     * @param tenantId  the tenant ID
     * @param startTime start of time range
     * @param endTime   end of time range
     * @return list of maps with endpoint, total_count, error_count
     */
    @Select("SELECT api_endpoint AS endpoint, " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN response_code >= 400 THEN 1 ELSE 0 END) AS errorCount, " +
            "AVG(response_time_ms) AS avgResponseTimeMs " +
            "FROM amazon_api_log " +
            "WHERE tenant_id = #{tenantId} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY api_endpoint " +
            "ORDER BY totalCount DESC")
    List<Map<String, Object>> selectApiStats(@Param("tenantId") Long tenantId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
