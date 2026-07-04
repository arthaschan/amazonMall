package cn.iocoder.yudao.module.amazon.ad.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRulePageReqVO;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRuleSaveReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdRuleDO;
import cn.iocoder.yudao.module.amazon.ad.service.AdRuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 广告规则")
@RestController
@RequestMapping("/amazon/ad/rule")
@Validated
public class AdRuleController {

    @Resource
    private AdRuleEngineService ruleEngineService;

    @PostMapping("/create")
    @Operation(summary = "创建规则")
    @PreAuthorize("@ss.hasPermission('amazon:ad:rule:create')")
    public CommonResult<Long> createRule(@Valid @RequestBody AdRuleSaveReqVO reqVO) {
        return success(ruleEngineService.createRule(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新规则")
    @PreAuthorize("@ss.hasPermission('amazon:ad:rule:update')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody AdRuleSaveReqVO reqVO) {
        ruleEngineService.updateRule(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除规则")
    @Parameter(name = "id", description = "规则 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:ad:rule:delete')")
    public CommonResult<Boolean> deleteRule(@RequestParam Long id) {
        ruleEngineService.deleteRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取规则详情")
    @PreAuthorize("@ss.hasPermission('amazon:ad:rule:query')")
    public CommonResult<AmazonAdRuleDO> getRule(@RequestParam Long id) {
        return success(ruleEngineService.getRule(id));
    }

    @GetMapping("/page")
    @Operation(summary = "规则分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:ad:rule:query')")
    public CommonResult<PageResult<AmazonAdRuleDO>> getRulePage(@Valid AdRulePageReqVO reqVO) {
        return success(ruleEngineService.getRulePage(reqVO));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行规则")
    @Parameter(name = "shopId", description = "店铺 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:ad:rule:execute')")
    public CommonResult<Boolean> executeRules(@RequestParam Long shopId) {
        ruleEngineService.executeRules(shopId);
        return success(true);
    }
}
