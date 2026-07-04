package cn.iocoder.yudao.module.amazon.review.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewPageReqVO;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewRespVO;
import cn.iocoder.yudao.module.amazon.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 评论管理")
@RestController
@RequestMapping("/amazon/review")
@Validated
public class ReviewController {

    @Resource
    private ReviewService reviewService;

    @GetMapping("/page")
    @Operation(summary = "评论分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:review:query')")
    public CommonResult<PageResult<ReviewRespVO>> getReviewPage(@Valid ReviewPageReqVO reqVO) {
        PageResult<AmazonReviewDO> page = reviewService.getReviewPage(reqVO);
        return success(BeanUtils.toBean(page, ReviewRespVO.class));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步评论")
    @io.swagger.v3.oas.annotations.Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "shopId", description = "店铺 ID", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "asin", description = "ASIN", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:review:sync')")
    public CommonResult<Boolean> syncReviews(@RequestParam Long shopId, @RequestParam String asin) {
        reviewService.syncReviews(shopId, asin);
        return success(true);
    }
}
