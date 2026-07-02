package cn.iocoder.yudao.module.amazon.research.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NichePageReqVO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonNicheDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonNicheMapper extends BaseMapperX<AmazonNicheDO> {

    default PageResult<AmazonNicheDO> selectPage(NichePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonNicheDO>()
                .likeIfPresent(AmazonNicheDO::getName, reqVO.getName())
                .eqIfPresent(AmazonNicheDO::getMarketplaceId, reqVO.getMarketplaceId())
                .eqIfPresent(AmazonNicheDO::getStatus, reqVO.getStatus())
                .orderByDesc(AmazonNicheDO::getId));
    }
}
