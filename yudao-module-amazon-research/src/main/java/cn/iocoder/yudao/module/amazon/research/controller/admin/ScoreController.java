package cn.iocoder.yudao.module.amazon.research.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.research.service.BsrSalesEstimatorService;
import cn.iocoder.yudao.module.amazon.research.service.FinancialProjectionService;
import cn.iocoder.yudao.module.amazon.research.service.SupplierMatchService;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonFinancialProjectionDO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonSupplierMatchDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 选品评分与估算")
@RestController
@RequestMapping("/amazon/research/score")
@Validated
public class ScoreController {

    @Resource
    private BsrSalesEstimatorService bsrSalesEstimatorService;

    @Resource
    private FinancialProjectionService financialProjectionService;

    @Resource
    private SupplierMatchService supplierMatchService;

    @GetMapping("/estimate-sales")
    @Operation(summary = "根据 BSR 估算月销量")
    @Parameters({
            @Parameter(name = "categoryId", description = "类目 ID", required = true),
            @Parameter(name = "marketplaceId", description = "站点 ID", required = true),
            @Parameter(name = "bsr", description = "BSR 排名", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:research:score:query')")
    public CommonResult<Integer> estimateSales(
            @RequestParam String categoryId,
            @RequestParam String marketplaceId,
            @RequestParam int bsr) {
        return success(bsrSalesEstimatorService.estimateMonthlySales(categoryId, marketplaceId, bsr));
    }

    @PostMapping("/generate-projection/{opportunityId}")
    @Operation(summary = "生成财务预测")
    @Parameter(name = "opportunityId", description = "产品机会 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:research:score:update')")
    public CommonResult<AmazonFinancialProjectionDO> generateProjection(@PathVariable Long opportunityId) {
        return success(financialProjectionService.generateProjection(opportunityId));
    }

    @GetMapping("/projection/{opportunityId}")
    @Operation(summary = "获取财务预测")
    @PreAuthorize("@ss.hasPermission('amazon:research:score:query')")
    public CommonResult<AmazonFinancialProjectionDO> getProjection(@PathVariable Long opportunityId) {
        return success(financialProjectionService.getProjection(opportunityId));
    }

    @PostMapping("/match-suppliers/{opportunityId}")
    @Operation(summary = "匹配供应商")
    @PreAuthorize("@ss.hasPermission('amazon:research:score:update')")
    public CommonResult<List<AmazonSupplierMatchDO>> matchSuppliers(@PathVariable Long opportunityId) {
        return success(supplierMatchService.matchSuppliers(opportunityId));
    }
}
