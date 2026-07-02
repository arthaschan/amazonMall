package cn.iocoder.yudao.module.amazon.research.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NichePageReqVO;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NicheRespVO;
import cn.iocoder.yudao.module.amazon.research.controller.admin.vo.NicheSaveReqVO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonNicheDO;
import cn.iocoder.yudao.module.amazon.research.service.NicheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 选品品类")
@RestController
@RequestMapping("/amazon/research/niche")
@Validated
public class NicheController {

    @Resource
    private NicheService nicheService;

    @PostMapping("/create")
    @Operation(summary = "创建品类")
    @PreAuthorize("@ss.hasPermission('amazon:research:niche:create')")
    public CommonResult<Long> createNiche(@Valid @RequestBody NicheSaveReqVO reqVO) {
        return success(nicheService.createNiche(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新品类")
    @PreAuthorize("@ss.hasPermission('amazon:research:niche:update')")
    public CommonResult<Boolean> updateNiche(@Valid @RequestBody NicheSaveReqVO reqVO) {
        nicheService.updateNiche(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除品类")
    @Parameter(name = "id", description = "品类 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:research:niche:delete')")
    public CommonResult<Boolean> deleteNiche(@RequestParam("id") Long id) {
        nicheService.deleteNiche(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得品类详情")
    @Parameter(name = "id", description = "品类 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:research:niche:query')")
    public CommonResult<NicheRespVO> getNiche(@RequestParam("id") Long id) {
        AmazonNicheDO niche = nicheService.getNiche(id);
        return success(BeanUtils.toBean(niche, NicheRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "品类分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:research:niche:query')")
    public CommonResult<PageResult<NicheRespVO>> getNichePage(@Valid NichePageReqVO reqVO) {
        var page = nicheService.getNichePage(reqVO);
        return success(BeanUtils.toBean(page, NicheRespVO.class));
    }

    @PostMapping("/recalculate-score")
    @Operation(summary = "重新计算全知评分")
    @Parameter(name = "id", description = "品类 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:research:niche:update')")
    public CommonResult<Boolean> recalculateScore(@RequestParam("id") Long id) {
        nicheService.recalculateScore(id);
        return success(true);
    }
}
