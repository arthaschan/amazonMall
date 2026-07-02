package cn.iocoder.yudao.module.amazon.report.service;

import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.AiChatRespVO;

/**
 * AI 数据对话 Service。
 * <p>支持自然语言查询店铺数据，返回结构化分析结果。</p>
 *
 * @author AmazonOps AI
 */
public interface AiChatService {

    /**
     * 处理用户的自然语言查询。
     *
     * @param shopId   店铺 ID
     * @param question 用户问题
     * @return AI 回答
     */
    AiChatRespVO chat(Long shopId, String question);
}
