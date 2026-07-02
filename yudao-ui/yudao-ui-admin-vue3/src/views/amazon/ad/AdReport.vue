<template>
  <!-- 筛选条件 -->
  <ContentWrap>
    <el-form :inline="true" class="-mb-15px" :model="queryParams" ref="queryFormRef">
      <el-form-item label="店铺" prop="shopId">
        <el-select v-model="queryParams.shopId" placeholder="全部店铺" clearable class="!w-240px">
          <el-option v-for="shop in shopList" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="广告类型" prop="campaignType">
        <el-select v-model="queryParams.campaignType" placeholder="全部类型" clearable class="!w-240px">
          <el-option label="SP - 商品推广" value="SP" />
          <el-option label="SB - 品牌推广" value="SB" />
          <el-option label="SD - 展示推广" value="SD" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 汇总卡片 -->
  <el-row :gutter="20" class="mb-20px">
    <el-col :span="5" v-for="card in summaryCards" :key="card.label">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold" :class="card.color">{{ card.value }}</div>
          <div class="text-gray-500 mt-5px">{{ card.label }}</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 花费 vs 销售额趋势图 -->
  <ContentWrap title="花费 vs 销售额趋势" class="mb-20px">
    <div ref="chartRef" class="h-400px"></div>
  </ContentWrap>

  <!-- 报告列表 -->
  <ContentWrap title="广告报告明细">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="活动名称" align="left" prop="campaignName" min-width="200" show-overflow-tooltip />
      <el-table-column label="花费" align="center" prop="spend" width="110">
        <template #default="scope">
          ${{ scope.row.spend || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="销售额" align="center" prop="sales" width="110">
        <template #default="scope">
          ${{ scope.row.sales || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="ACoS" align="center" prop="acos" width="90">
        <template #default="scope">
          <span :class="scope.row.acos > 30 ? 'text-danger' : 'text-success'">
            {{ scope.row.acos || '0.00' }}%
          </span>
        </template>
      </el-table-column>
      <el-table-column label="ROAS" align="center" prop="roas" width="90">
        <template #default="scope">
          {{ scope.row.roas || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="点击" align="center" prop="clicks" width="80" />
      <el-table-column label="转化" align="center" prop="conversions" width="80" />
      <el-table-column label="CPA" align="center" prop="cpa" width="100">
        <template #default="scope">
          ${{ scope.row.cpa || '0.00' }}
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { AdReportApi } from '@/api/amazon/ad'
import { ShopApi } from '@/api/amazon/shop'

const message = useMessage()

/** 店铺列表 */
const shopList = ref([])
const dateRange = ref([])

/** 查询参数 */
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  campaignType: undefined,
  startTime: undefined,
  endTime: undefined
})
const queryFormRef = ref()

/** 汇总数据 */
const summaryData = ref({
  totalSpend: '0.00',
  totalSales: '0.00',
  acos: '0.00',
  roas: '0.00',
  totalClicks: 0
})

const summaryCards = computed(() => [
  { label: '总花费', value: `$${summaryData.value.totalSpend}`, color: 'text-danger' },
  { label: '总销售额', value: `$${summaryData.value.totalSales}`, color: 'text-success' },
  { label: 'ACoS', value: `${summaryData.value.acos}%`, color: 'text-warning' },
  { label: 'ROAS', value: summaryData.value.roas, color: 'text-primary' },
  { label: '总点击', value: summaryData.value.totalClicks, color: 'text-info' }
])

/** 列表数据 */
const loading = ref(true)
const total = ref(0)
const list = ref([])

/** 图表 */
const chartRef = ref()
let chartInstance = null

/** 获取列表 */
const getList = async () => {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startTime = dateRange.value[0]
      queryParams.endTime = dateRange.value[1]
    } else {
      queryParams.startTime = undefined
      queryParams.endTime = undefined
    }
    const data = await AdReportApi.getAdReportPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 加载汇总 */
const loadSummary = async () => {
  try {
    summaryData.value = await AdReportApi.getAdReportSummary(queryParams)
  } catch (error) {
    console.error('加载汇总数据失败', error)
  }
}

/** 加载趋势图 */
const loadChart = async () => {
  try {
    const data = await AdReportApi.getAdReportPage({
      ...queryParams,
      pageNo: 1,
      pageSize: 1000,
      _trend: true
    })
    renderChart(data)
  } catch (error) {
    console.error('加载趋势数据失败', error)
  }
}

/** 渲染图表 */
const renderChart = (data) => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['花费', '销售额'] },
    xAxis: { type: 'category', data: data.dates || [] },
    yAxis: [
      { type: 'value', name: '花费($)' },
      { type: 'value', name: '销售额($)', position: 'right' }
    ],
    series: [
      {
        name: '花费',
        type: 'bar',
        data: data.spendList || [],
        itemStyle: { color: '#f56c6c' }
      },
      {
        name: '销售额',
        type: 'line',
        yAxisIndex: 1,
        data: data.salesList || [],
        itemStyle: { color: '#67c23a' }
      }
    ]
  })
}

/** 加载店铺列表 */
const loadShopList = async () => {
  try {
    const data = await ShopApi.getShopPage({ pageNo: 1, pageSize: 100 })
    shopList.value = data.list
  } catch (error) {
    console.error('加载店铺列表失败', error)
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  loadSummary()
  loadChart()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  dateRange.value = []
  handleQuery()
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
  loadSummary()
  loadChart()
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>
