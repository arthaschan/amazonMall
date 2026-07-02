package cn.iocoder.yudao.module.amazon.ad.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdReportPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.SearchTermPageReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdSearchTermDO;
import cn.iocoder.yudao.module.amazon.ad.service.AdReportService;
import cn.iocoder.yudao.module.amazon.ad.service.AdSearchTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 广告报表")
@RestController
@RequestMapping("/amazon/ad/report")
@Validated
public class AdReportController {

    @Resource
    private AdReportService reportService;

    @Resource
    private AdSearchTermService searchTermService;

    @GetMapping("/page")
    @Operation(summary = "广告日报分页")
    @PreAuthorize("@ss.hasPermission('amazon:ad:report:query')")
    public CommonResult<PageResult<AmazonAdReportDailyDO>> getReportPage(@Valid AdReportPageReqVO reqVO) {
        return success(reportService.getReportPage(reqVO));
    }

    @GetMapping("/search-terms")
    @Operation(summary = "搜索词报告")
    @PreAuthorize("@ss.hasPermission('amazon:ad:report:query')")
    public CommonResult<PageResult<AmazonAdSearchTermDO>> getSearchTermPage(@Valid SearchTermPageReqVO reqVO) {
        return success(searchTermService.getSearchTermPage(reqVO));
    }
}
