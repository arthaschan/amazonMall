package cn.iocoder.yudao.module.amazon.shop.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonShopPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis Plus mapper for {@link AmazonShopDO}.
 *
 * @author AmazonOps AI
 */
@Mapper
public interface AmazonShopMapper extends BaseMapperX<AmazonShopDO> {

    /**
     * Paginated query of shops with optional filters.
     *
     * @param reqVO the page request with filter parameters
     * @return paginated result
     */
    default PageResult<AmazonShopDO> selectPage(AmazonShopPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonShopDO>()
                .likeIfPresent(AmazonShopDO::getShopName, reqVO.getShopName())
                .eqIfPresent(AmazonShopDO::getMarketplaceId, reqVO.getMarketplaceId())
                .eqIfPresent(AmazonShopDO::getSellerId, reqVO.getSellerId())
                .eqIfPresent(AmazonShopDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AmazonShopDO::getCountryCode, reqVO.getCountryCode())
                .betweenIfPresent(AmazonShopDO::getCreateTime, reqVO.getCreateTimeStart(), reqVO.getCreateTimeEnd())
                .orderByDesc(AmazonShopDO::getId));
    }

    /**
     * Finds a shop by seller ID and marketplace ID.
     *
     * @param sellerId      the Amazon seller ID
     * @param marketplaceId the marketplace ID
     * @return the shop, or {@code null} if not found
     */
    default AmazonShopDO selectBySellerIdAndMarketplaceId(String sellerId, String marketplaceId) {
        return selectOne(AmazonShopDO::getSellerId, sellerId,
                AmazonShopDO::getMarketplaceId, marketplaceId);
    }

    /**
     * Lists all shops with a specific status.
     *
     * @param status the status to filter by
     * @return list of matching shops
     */
    default List<AmazonShopDO> selectListByStatus(Integer status) {
        return selectList(AmazonShopDO::getStatus, status);
    }

    /**
     * Lists all shops for a given tenant.
     *
     * @param tenantId the tenant ID
     * @return list of shops
     */
    default List<AmazonShopDO> selectListByTenantId(Long tenantId) {
        return selectList(AmazonShopDO::getTenantId, tenantId);
    }
}
