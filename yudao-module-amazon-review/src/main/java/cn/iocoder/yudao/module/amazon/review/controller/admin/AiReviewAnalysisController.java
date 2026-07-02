package cn.iocoder.yudao.module.amazon.review.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.review.service.AiReviewAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 评论分析")
@RestController
@RequestMapping("/amazon/review/ai")
@Validated
public class AiReviewAnalysisController {

    @Resource
    private AiReviewAnalysisService analysisService;

    @PostMapping("/analyze")
    @Operation(summary = "AI 分析单条评论")
    @Parameter(name = "reviewId", description = "评论 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:review:ai:analyze')")
    public CommonResult<Boolean> analyzeReview(@RequestParam Long reviewId) {
        analysisService.analyzeReview(reviewId);
        return success(true);
    }

    @PostMapping("/analyze-asin")
    @Operation(summary = "AI 批量分析 ASIN 评论")
    @PreAuthorize("@ss.hasPermission('amazon:review:ai:analyze')")
    public CommonResult<Boolean> analyzeByAsin(@RequestParam Long shopId, @RequestParam String asin) {
        analysisService.analyzeByAsin(shopId, asin);
        return success(true);
    }

    @GetMapping("/aggregate")
    @Operation(summary = "ASIN 评论聚合分析")
    @PreAuthorize("@ss.hasPermission('amazon:review:ai:query')")
    public CommonResult<Map<String, Object>> getAggregateAnalysis(
            @RequestParam Long shopId, @RequestParam String asin) {
        return success(analysisService.getAggregateAnalysis(shopId, asin));
    }
}
