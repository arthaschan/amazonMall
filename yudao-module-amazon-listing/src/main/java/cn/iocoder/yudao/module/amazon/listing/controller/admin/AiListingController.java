package cn.iocoder.yudao.module.amazon.listing.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.AiGenerateReqVO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;
import cn.iocoder.yudao.module.amazon.listing.service.AiListingGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI Listing 生成")
@RestController
@RequestMapping("/amazon/listing/ai")
@Validated
public class AiListingController {

    @Resource
    private AiListingGeneratorService aiListingGeneratorService;

    @PostMapping("/generate")
    @Operation(summary = "AI 生成 Listing")
    @PreAuthorize("@ss.hasPermission('amazon:listing:ai:generate')")
    public CommonResult<AmazonListingVersionDO> generate(@Valid @RequestBody AiGenerateReqVO reqVO) {
        return success(aiListingGeneratorService.generate(reqVO.getProductId(), reqVO.getPrompt()));
    }

    @PostMapping("/optimize")
    @Operation(summary = "AI 优化 Listing")
    @PreAuthorize("@ss.hasPermission('amazon:listing:ai:generate')")
    public CommonResult<AmazonListingVersionDO> optimize(@Valid @RequestBody AiGenerateReqVO reqVO) {
        return success(aiListingGeneratorService.optimize(reqVO.getProductId(), reqVO.getPrompt()));
    }
}
