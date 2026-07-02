package cn.iocoder.yudao.module.amazon.shop.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonMarketplaceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis Plus mapper for {@link AmazonMarketplaceDO}.
 *
 * @author AmazonOps AI
 */
@Mapper
public interface AmazonMarketplaceMapper extends BaseMapperX<AmazonMarketplaceDO> {

    /**
     * Finds a marketplace by its Amazon marketplace ID.
     *
     * @param marketplaceId the Amazon marketplace ID (e.g. ATVPDKIKX0DER)
     * @return the marketplace record, or {@code null} if not found
     */
    default AmazonMarketplaceDO selectByMarketplaceId(String marketplaceId) {
        return selectOne(AmazonMarketplaceDO::getMarketplaceId, marketplaceId);
    }

    /**
     * Lists all marketplaces in a given region.
     *
     * @param region the region code (NA, EU, FE)
     * @return list of marketplaces
     */
    default List<AmazonMarketplaceDO> selectListByRegion(String region) {
        return selectList(AmazonMarketplaceDO::getRegion, region);
    }

    /**
     * Lists all marketplaces for a given country.
     *
     * @param country the ISO country code
     * @return list of marketplaces
     */
    default List<AmazonMarketplaceDO> selectListByCountry(String country) {
        return selectList(AmazonMarketplaceDO::getCountry, country);
    }
}
