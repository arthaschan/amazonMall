package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplatePageReqVO;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplateSaveReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonCustomerTemplateDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonCustomerTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class CustomerTemplateServiceImpl implements CustomerTemplateService {

    @Resource
    private AmazonCustomerTemplateMapper templateMapper;

    @Override
    public Long createTemplate(CustomerTemplateSaveReqVO reqVO) {
        var template = BeanUtils.toBean(reqVO, AmazonCustomerTemplateDO.class);
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateTemplate(CustomerTemplateSaveReqVO reqVO) {
        templateMapper.updateById(BeanUtils.toBean(reqVO, AmazonCustomerTemplateDO.class));
    }

    @Override
    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    public AmazonCustomerTemplateDO getTemplate(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonCustomerTemplateDO> getTemplatePage(CustomerTemplatePageReqVO reqVO) {
        return templateMapper.selectPage(reqVO);
    }
}
