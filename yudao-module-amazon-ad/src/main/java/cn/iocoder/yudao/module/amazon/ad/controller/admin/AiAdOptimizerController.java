package cn.iocoder.yudao.module.amazon.ad.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.ad.service.AiAdOptimizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 广告优化")
@RestController
@RequestMapping("/amazon/ad/ai")
@Validated
public class AiAdOptimizerController {

    @Resource
    private AiAdOptimizerService aiAdOptimizerService;

    @GetMapping("/analyze")
    @Operation(summary = "AI 分析并生成优化建议")
    @Parameter(name = "shopId", description = "店铺 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:ad:ai:analyze')")
    public CommonResult<Map<String, Object>> analyzeAndSuggest(@RequestParam Long shopId) {
        return success(aiAdOptimizerService.analyzeAndSuggest(shopId));
    }

    @PostMapping("/auto-optimize")
    @Operation(summary = "AI 自动优化")
    @Parameter(name = "shopId", description = "店铺 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:ad:ai:optimize')")
    public CommonResult<Boolean> autoOptimize(@RequestParam Long shopId) {
        aiAdOptimizerService.autoOptimize(shopId);
        return success(true);
    }
}
