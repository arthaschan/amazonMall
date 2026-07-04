package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplatePageReqVO;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplateSaveReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonCustomerTemplateDO;

import javax.validation.Valid;

/**
 * 客服消息模板 Service。
 *
 * @author AmazonOps AI
 */
public interface CustomerTemplateService {

    Long createTemplate(@Valid CustomerTemplateSaveReqVO reqVO);

    void updateTemplate(@Valid CustomerTemplateSaveReqVO reqVO);

    void deleteTemplate(Long id);

    AmazonCustomerTemplateDO getTemplate(Long id);

    PageResult<AmazonCustomerTemplateDO> getTemplatePage(CustomerTemplatePageReqVO reqVO);
}
