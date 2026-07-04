package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.SearchTermPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdSearchTermDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdSearchTermMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdSearchTermServiceImpl implements AdSearchTermService {

    @Resource
    private AmazonAdSearchTermMapper searchTermMapper;

    @Override
    public PageResult<AmazonAdSearchTermDO> getSearchTermPage(SearchTermPageReqVO reqVO) {
        return searchTermMapper.selectPage(reqVO);
    }

    @Override
    public void tagSearchTerms(Long shopId, Long campaignId) {
        // TODO: AI 分析搜索词并打标
    }
}
