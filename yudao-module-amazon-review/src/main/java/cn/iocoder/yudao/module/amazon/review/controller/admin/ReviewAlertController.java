package cn.iocoder.yudao.module.amazon.review.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.ReviewAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewAlertDO;
import cn.iocoder.yudao.module.amazon.review.service.ReviewAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 评论预警")
@RestController
@RequestMapping("/amazon/review/alert")
@Validated
public class ReviewAlertController {

    @Resource
    private ReviewAlertService alertService;

    @GetMapping("/page")
    @Operation(summary = "预警分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:review:alert:query')")
    public CommonResult<PageResult<AmazonReviewAlertDO>> getAlertPage(@Valid ReviewAlertPageReqVO reqVO) {
        return success(alertService.getAlertPage(reqVO));
    }

    @PostMapping("/acknowledge")
    @Operation(summary = "确认预警")
    @Parameter(name = "id", description = "预警 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:review:alert:update')")
    public CommonResult<Boolean> acknowledge(@RequestParam Long id) {
        alertService.acknowledgeAlert(id);
        return success(true);
    }
}
