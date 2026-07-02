package com.yudao.module.amazon.report.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.WeeklyReportPageReqVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonWeeklyReportDO;
import cn.iocoder.yudao.module.amazon.report.dal.mysql.AmazonWeeklyReportMapper;
import cn.iocoder.yudao.module.amazon.report.service.AiWeeklyReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link AiWeeklyReportServiceImpl} 单元测试
 *
 * @author AmazonOps AI
 */
@ExtendWith(MockitoExtension.class)
class AiWeeklyReportServiceImplTest {

    @Mock
    private AmazonWeeklyReportMapper reportMapper;

    @InjectMocks
    private AiWeeklyReportServiceImpl aiWeeklyReportService;

    // ---------- 辅助方法 ----------

    private AmazonWeeklyReportDO buildReport(Long shopId, String week, Integer status) {
        AmazonWeeklyReportDO report = new AmazonWeeklyReportDO();
        report.setId(1L);
        report.setShopId(shopId);
        report.setReportWeek(week);
        report.setStatus(status);
        return report;
    }

    // ---------- generateReport ----------

    @Test
    @DisplayName("已有已完成报告(status=1) - 直接返回，不做更新")
    void testGenerateReport_ExistingCompleted() {
        Long shopId = 1L;
        String week = "2024-W03";
        AmazonWeeklyReportDO existing = buildReport(shopId, week, 1);

        when(reportMapper.selectByWeek(shopId, week)).thenReturn(existing);

        AmazonWeeklyReportDO result = aiWeeklyReportService.generateReport(shopId, week);

        assertThat(result).isSameAs(existing);
        verify(reportMapper, never()).insert(any());
        verify(reportMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("不存在报告 - 新建并 insert，status=1")
    void testGenerateReport_NewReport() {
        Long shopId = 1L;
        String week = "2024-W03";

        when(reportMapper.selectByWeek(shopId, week)).thenReturn(null);

        AmazonWeeklyReportDO result = aiWeeklyReportService.generateReport(shopId, week);

        // 验证 insert 被调用
        ArgumentCaptor<AmazonWeeklyReportDO> captor = ArgumentCaptor.forClass(AmazonWeeklyReportDO.class);
        verify(reportMapper).insert(captor.capture());
        AmazonWeeklyReportDO inserted = captor.getValue();

        assertThat(inserted.getShopId()).isEqualTo(shopId);
        assertThat(inserted.getReportWeek()).isEqualTo(week);
        assertThat(inserted.getStatus()).isEqualTo(1);
        assertThat(inserted.getReportData()).isNotNull();
        assertThat(inserted.getReportData().getTotalSales()).isZero();
        assertThat(inserted.getAiSummary()).isNotBlank();

        // 返回的是同一个对象
        assertThat(result).isSameAs(inserted);
    }

    @Test
    @DisplayName("已有生成中报告(status=0) - 使用 updateById 更新")
    void testGenerateReport_ExistingGenerating() {
        Long shopId = 1L;
        String week = "2024-W03";
        AmazonWeeklyReportDO existing = buildReport(shopId, week, 0);

        when(reportMapper.selectByWeek(shopId, week)).thenReturn(existing);

        AmazonWeeklyReportDO result = aiWeeklyReportService.generateReport(shopId, week);

        // 验证 updateById 被调用
        verify(reportMapper).updateById(any(AmazonWeeklyReportDO.class));
        verify(reportMapper, never()).insert(any());

        // 状态已更新为 1
        assertThat(result.getStatus()).isEqualTo(1);
    }

    // ---------- getReport ----------

    @Test
    @DisplayName("getReport - 委托到 mapper.selectByWeek")
    void testGetReport() {
        Long shopId = 1L;
        String week = "2024-W03";
        AmazonWeeklyReportDO expected = buildReport(shopId, week, 1);

        when(reportMapper.selectByWeek(shopId, week)).thenReturn(expected);

        AmazonWeeklyReportDO result = aiWeeklyReportService.getReport(shopId, week);

        assertThat(result).isSameAs(expected);
        verify(reportMapper).selectByWeek(shopId, week);
    }

    // ---------- getReportPage ----------

    @Test
    @DisplayName("getReportPage - 委托到 mapper.selectPage")
    void testGetReportPage() {
        WeeklyReportPageReqVO reqVO = new WeeklyReportPageReqVO();
        reqVO.setShopId(1L);
        reqVO.setReportWeek("2024-W03");

        PageResult<AmazonWeeklyReportDO> expected = new PageResult<>(Collections.emptyList(), 0L);
        when(reportMapper.selectPage(reqVO)).thenReturn(expected);

        PageResult<AmazonWeeklyReportDO> result = aiWeeklyReportService.getReportPage(reqVO);

        assertThat(result).isSameAs(expected);
        verify(reportMapper).selectPage(reqVO);
    }
}
