package cn.iocoder.yudao.module.amazon.ad.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRulePageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonAdRuleMapper extends BaseMapperX<AmazonAdRuleDO> {

    default PageResult<AmazonAdRuleDO> selectPage(AdRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonAdRuleDO>()
                .eqIfPresent(AmazonAdRuleDO::getShopId, reqVO.getShopId())
                .likeIfPresent(AmazonAdRuleDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(AmazonAdRuleDO::getScope, reqVO.getScope())
                .eqIfPresent(AmazonAdRuleDO::getStatus, reqVO.getStatus())
                .orderByDesc(AmazonAdRuleDO::getId));
    }

    default List<AmazonAdRuleDO> selectActiveRules(Long shopId) {
        return selectList(new LambdaQueryWrapperX<AmazonAdRuleDO>()
                .eq(AmazonAdRuleDO::getShopId, shopId)
                .eq(AmazonAdRuleDO::getStatus, 1));
    }
}
