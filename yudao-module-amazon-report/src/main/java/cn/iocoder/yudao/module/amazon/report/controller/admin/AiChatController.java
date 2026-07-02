package cn.iocoder.yudao.module.amazon.report.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.AiChatReqVO;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.AiChatRespVO;
import cn.iocoder.yudao.module.amazon.report.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 数据对话")
@RestController
@RequestMapping("/amazon/report/ai-chat")
@Validated
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @PostMapping("/ask")
    @Operation(summary = "AI 数据问答")
    @PreAuthorize("@ss.hasPermission('amazon:report:ai-chat:query')")
    public CommonResult<AiChatRespVO> ask(@Valid @RequestBody AiChatReqVO reqVO) {
        return success(aiChatService.chat(reqVO.getShopId(), reqVO.getQuestion()));
    }
}
