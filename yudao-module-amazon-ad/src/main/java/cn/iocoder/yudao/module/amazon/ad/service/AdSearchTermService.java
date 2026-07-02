package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.SearchTermPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdSearchTermDO;

/**
 * 广告搜索词 Service。
 *
 * @author AmazonOps AI
 */
public interface AdSearchTermService {

    PageResult<AmazonAdSearchTermDO> getSearchTermPage(SearchTermPageReqVO reqVO);

    /** AI 标签分类 */
    void tagSearchTerms(Long shopId, Long campaignId);
}
