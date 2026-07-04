package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NichePageReqVO;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NicheSaveReqVO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonNicheDO;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonNicheMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 选品品类 Service 实现。
 *
 * @author AmazonOps AI
 */
@Service
@Validated
public class NicheServiceImpl implements NicheService {

    @Resource
    private AmazonNicheMapper nicheMapper;

    @Resource
    private OmniscientScoreCalculatorService scoreCalculator;

    @Override
    public Long createNiche(NicheSaveReqVO reqVO) {
        AmazonNicheDO niche = BeanUtils.toBean(reqVO, AmazonNicheDO.class);
        niche.setStatus(0); // 草稿
        nicheMapper.insert(niche);
        return niche.getId();
    }

    @Override
    public void updateNiche(NicheSaveReqVO reqVO) {
        validateNicheExists(reqVO.getId());
        AmazonNicheDO updateObj = BeanUtils.toBean(reqVO, AmazonNicheDO.class);
        nicheMapper.updateById(updateObj);
    }

    @Override
    public void deleteNiche(Long id) {
        validateNicheExists(id);
        nicheMapper.deleteById(id);
    }

    @Override
    public AmazonNicheDO getNiche(Long id) {
        return nicheMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonNicheDO> getNichePage(NichePageReqVO reqVO) {
        return nicheMapper.selectPage(reqVO);
    }

    @Override
    public void recalculateScore(Long id) {
        AmazonNicheDO niche = validateNicheExists(id);
        BigDecimal score = scoreCalculator.calculate(niche);
        niche.setOmniscientScore(score);
        nicheMapper.updateById(niche);
    }

    private AmazonNicheDO validateNicheExists(Long id) {
        AmazonNicheDO niche = nicheMapper.selectById(id);
        if (niche == null) {
            throw exception(new cn.iocoder.yudao.framework.common.exception.ErrorCode(1_200_001_000, "品类不存在"));
        }
        return niche;
    }
}
