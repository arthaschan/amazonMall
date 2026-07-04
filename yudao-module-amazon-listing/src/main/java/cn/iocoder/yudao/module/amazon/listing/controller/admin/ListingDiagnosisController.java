package cn.iocoder.yudao.module.amazon.listing.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;
import cn.iocoder.yudao.module.amazon.listing.service.ListingDiagnosisService;
import cn.iocoder.yudao.module.amazon.listing.service.ListingVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - Listing 诊断")
@RestController
@RequestMapping("/amazon/listing/diagnosis")
@Validated
public class ListingDiagnosisController {

    @Resource
    private ListingDiagnosisService diagnosisService;

    @Resource
    private ListingVersionService versionService;

    @GetMapping("/diagnose")
    @Operation(summary = "Listing 诊断")
    @Parameter(name = "productId", description = "产品 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:listing:diagnosis:query')")
    public CommonResult<Map<String, Object>> diagnose(@RequestParam Long productId) {
        return success(diagnosisService.diagnose(productId));
    }

    @GetMapping("/versions")
    @Operation(summary = "获取 Listing 版本列表")
    @Parameter(name = "productId", description = "产品 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:listing:diagnosis:query')")
    public CommonResult<List<AmazonListingVersionDO>> getVersions(@RequestParam Long productId) {
        return success(versionService.getVersions(productId));
    }

    @PostMapping("/rollback")
    @Operation(summary = "回滚到指定版本")
    @PreAuthorize("@ss.hasPermission('amazon:listing:product:update')")
    public CommonResult<Boolean> rollback(@RequestParam Long productId, @RequestParam Integer versionNum) {
        versionService.rollback(productId, versionNum);
        return success(true);
    }
}
