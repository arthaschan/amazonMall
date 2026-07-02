package com.yudao.module.amazon.report.service;

import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.AiChatRespVO;
import cn.iocoder.yudao.module.amazon.report.service.AiChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * {@link AiChatServiceImpl} 单元测试
 *
 * @author AmazonOps AI
 */
class AiChatServiceImplTest {

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatServiceImpl();
    }

    @Test
    @DisplayName("回答中包含用户提问内容")
    void testChat_ReturnsAnswerContainingQuestion() {
        String question = "本周销售额是多少";

        AiChatRespVO resp = aiChatService.chat(1L, question);

        assertThat(resp.getAnswer()).contains(question);
    }

    @Test
    @DisplayName("dataReference 为 null")
    void testChat_DataReferenceIsNull() {
        AiChatRespVO resp = aiChatService.chat(1L, "任意问题");

        assertThat(resp.getDataReference()).isNull();
    }

    @Test
    @DisplayName("空问题不抛异常")
    void testChat_EmptyQuestion() {
        assertThatNoException().isThrownBy(() -> aiChatService.chat(1L, ""));
    }
}
