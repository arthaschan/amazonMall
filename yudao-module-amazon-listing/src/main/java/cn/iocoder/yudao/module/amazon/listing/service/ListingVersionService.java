package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;

import java.util.List;

/**
 * Listing 版本管理服务。
 *
 * @author AmazonOps AI
 */
public interface ListingVersionService {

    List<AmazonListingVersionDO> getVersions(Long productId);

    AmazonListingVersionDO getVersion(Long versionId);

    /** 回滚到指定版本 */
    void rollback(Long productId, Integer versionNum);
}
