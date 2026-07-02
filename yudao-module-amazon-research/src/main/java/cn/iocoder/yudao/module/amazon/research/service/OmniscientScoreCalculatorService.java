package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonNicheDO;

import java.math.BigDecimal;

/**
 * 全知评分（Omniscient Score）计算器。
 * <p>根据十维评分和权重矩阵，加权计算综合评分。</p>
 *
 * @author AmazonOps AI
 */
public interface OmniscientScoreCalculatorService {

    /**
     * 计算综合评分。
     *
     * @param niche 品类数据（含十维评分和权重）
     * @return 0-100 加权评分
     */
    BigDecimal calculate(AmazonNicheDO niche);
}
