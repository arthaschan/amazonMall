package cn.iocoder.yudao.module.amazon.research.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.OpportunityPageReqVO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonProductOpportunityDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonProductOpportunityMapper extends BaseMapperX<AmazonProductOpportunityDO> {

    default PageResult<AmazonProductOpportunityDO> selectPage(OpportunityPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonProductOpportunityDO>()
                .eqIfPresent(AmazonProductOpportunityDO::getNicheId, reqVO.getNicheId())
                .likeIfPresent(AmazonProductOpportunityDO::getAsin, reqVO.getAsin())
                .orderByDesc(AmazonProductOpportunityDO::getEstimatedMonthlySales));
    }

    default List<AmazonProductOpportunityDO> selectByNicheId(Long nicheId) {
        return selectList(AmazonProductOpportunityDO::getNicheId, nicheId);
    }
}
