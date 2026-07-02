package cn.iocoder.yudao.module.amazon.review.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplatePageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonCustomerTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonCustomerTemplateMapper extends BaseMapperX<AmazonCustomerTemplateDO> {

    default PageResult<AmazonCustomerTemplateDO> selectPage(CustomerTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonCustomerTemplateDO>()
                .likeIfPresent(AmazonCustomerTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(AmazonCustomerTemplateDO::getTemplateType, reqVO.getTemplateType())
                .eqIfPresent(AmazonCustomerTemplateDO::getLanguage, reqVO.getLanguage())
                .orderByDesc(AmazonCustomerTemplateDO::getId));
    }

    default List<AmazonCustomerTemplateDO> selectByType(String templateType) {
        return selectList(AmazonCustomerTemplateDO::getTemplateType, templateType);
    }
}
