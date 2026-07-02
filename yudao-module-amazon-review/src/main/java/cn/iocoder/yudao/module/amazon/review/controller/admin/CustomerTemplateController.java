package cn.iocoder.yudao.module.amazon.review.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplatePageReqVO;
import cn.iocoder.yudao.module.amazon.review.controller.admin.vo.CustomerTemplateSaveReqVO;
import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonCustomerTemplateDO;
import cn.iocoder.yudao.module.amazon.review.service.CustomerTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 客服消息模板")
@RestController
@RequestMapping("/amazon/review/template")
@Validated
public class CustomerTemplateController {

    @Resource
    private CustomerTemplateService templateService;

    @PostMapping("/create")
    @Operation(summary = "创建模板")
    @PreAuthorize("@ss.hasPermission('amazon:review:template:create')")
    public CommonResult<Long> create(@Valid @RequestBody CustomerTemplateSaveReqVO reqVO) {
        return success(templateService.createTemplate(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新模板")
    @PreAuthorize("@ss.hasPermission('amazon:review:template:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody CustomerTemplateSaveReqVO reqVO) {
        templateService.updateTemplate(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模板")
    @Parameter(name = "id", description = "模板 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:review:template:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        templateService.deleteTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取模板详情")
    @PreAuthorize("@ss.hasPermission('amazon:review:template:query')")
    public CommonResult<AmazonCustomerTemplateDO> get(@RequestParam Long id) {
        return success(templateService.getTemplate(id));
    }

    @GetMapping("/page")
    @Operation(summary = "模板分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:review:template:query')")
    public CommonResult<PageResult<AmazonCustomerTemplateDO>> page(@Valid CustomerTemplatePageReqVO reqVO) {
        return success(templateService.getTemplatePage(reqVO));
    }
}
