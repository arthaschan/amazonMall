package cn.iocoder.yudao.module.amazon.ad.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.CampaignPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.CampaignRespVO;
import cn.iocoder.yudao.module.amazon.ad.service.AdCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 广告活动")
@RestController
@RequestMapping("/amazon/ad/campaign")
@Validated
public class AdCampaignController {

    @Resource
    private AdCampaignService campaignService;

    @GetMapping("/get")
    @Operation(summary = "获取广告活动详情")
    @Parameter(name = "id", description = "活动 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:ad:campaign:query')")
    public CommonResult<CampaignRespVO> getCampaign(@RequestParam Long id) {
        var campaign = campaignService.getCampaign(id);
        return success(BeanUtils.toBean(campaign, CampaignRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "广告活动分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:ad:campaign:query')")
    public CommonResult<PageResult<CampaignRespVO>> getCampaignPage(@Valid CampaignPageReqVO reqVO) {
        var page = campaignService.getCampaignPage(reqVO);
        return success(BeanUtils.toBean(page, CampaignRespVO.class));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步广告活动")
    @Parameter(name = "shopId", description = "店铺 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:ad:campaign:sync')")
    public CommonResult<Boolean> syncCampaigns(@RequestParam Long shopId) {
        campaignService.syncCampaigns(shopId);
        return success(true);
    }
}
