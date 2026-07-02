package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NichePageReqVO;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NicheSaveReqVO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonNicheDO;

import jakarta.validation.Valid;

/**
 * 选品品类 Service 接口。
 *
 * @author AmazonOps AI
 */
public interface NicheService {

    Long createNiche(@Valid NicheSaveReqVO reqVO);

    void updateNiche(@Valid NicheSaveReqVO reqVO);

    void deleteNiche(Long id);

    AmazonNicheDO getNiche(Long id);

    PageResult<AmazonNicheDO> getNichePage(NichePageReqVO reqVO);

    /** 触发全知评分重新计算 */
    void recalculateScore(Long id);
}
