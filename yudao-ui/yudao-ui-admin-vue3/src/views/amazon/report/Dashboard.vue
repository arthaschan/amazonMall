<template>
  <!-- 筛选条件 -->
  <ContentWrap>
    <el-form :inline="true" class="-mb-15px">
      <el-form-item label="店铺">
        <el-select v-model="queryParams.shopId" placeholder="全部店铺" clearable class="!w-240px">
          <el-option v-for="shop in shopList" :key="shop.id" :label="shop.name" :value="shop.id" />
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
        <el-button type="primary" @click="loadData"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 统计卡片 -->
  <el-row :gutter="20" class="mb-20px">
    <el-col :span="4" v-for="card in statCards" :key="card.label">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-24px font-bold" :class="card.color">{{ card.value }}</div>
          <div class="text-gray-500 mt-5px text-14px">{{ card.label }}</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 销售趋势图 -->
  <ContentWrap title="销售趋势" class="mb-20px">
    <div ref="salesChartRef" class="h-400px"></div>
  </ContentWrap>

  <el-row :gutter="20">
    <!-- Top 10 商品 -->
    <el-col :span="14">
      <ContentWrap title="Top 10 商品">
        <el-table :data="topProducts" v-loading="productsLoading">
          <el-table-column label="排名" align="center" width="70">
            <template #default="scope">
              <el-tag :type="scope.$index < 3 ? 'danger' : 'info'" round>
                {{ scope.$index + 1 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="SKU" align="center" prop="sku" width="120" />
          <el-table-column label="销售额" align="center" prop="sales" width="110">
            <template #default="scope">${{ scope.row.sales }}</template>
          </el-table-column>
          <el-table-column label="订单" align="center" prop="orders" width="80" />
          <el-table-column label="利润" align="center" prop="profit" width="110">
            <template #default="scope">
              <span :class="scope.row.profit > 0 ? 'text-success' : 'text-danger'">
                ${{ scope.row.profit }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="BSR" align="center" prop="bsrRank" width="90">
            <template #default="scope">
              #{{ scope.row.bsrRank || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </ContentWrap>
    </el-col>

    <!-- 站点销售分布 -->
    <el-col :span="10">
      <ContentWrap title="站点销售分布">
        <div ref="pieChartRef" class="h-350px"></div>
      </ContentWrap>
    </el-col>
  </el-row>

  <!-- 最近预警 -->
  <ContentWrap title="最近预警" class="mt-20px">
    <el-table :data="recentAlerts" v-loading="alertsLoading">
      <el-table-column label="预警类型" align="center" prop="type" width="120">
        <template #default="scope">
          <el-tag :type="getAlertTag(scope.row.type)">{{ scope.row.typeLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预警内容" align="left" prop="content" min-width="250" show-overflow-tooltip />
      <el-table-column
        label="触发时间"
        align="center"
        prop="triggerTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'READ' ? 'info' : 'danger'">
            {{ scope.row.status === 'READ' ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { DashboardApi } from '@/api/amazon/report'
import { ShopApi } from '@/api/amazon/shop'

const shopList = ref([])
const dateRange = ref([])
const queryParams = reactive({
  shopId: undefined,
  startTime: undefined,
  endTime: undefined
})

/** 统计数据 */
const metrics = ref({
  totalSales: '0.00',
  totalOrders: 0,
  totalProfit: '0.00',
  avgACoS: '0.00',
  conversionRate: '0.00',
  bsrChanges: 0
})

const statCards = computed(() => [
  { label: '总销售额', value: `$${metrics.value.totalSales}`, color: 'text-success' },
  { label: '总订单数', value: metrics.value.totalOrders, color: 'text-primary' },
  { label: '总利润', value: `$${metrics.value.totalProfit}`, color: 'text-success' },
  { label: '平均ACoS', value: `${metrics.value.avgACoS}%`, color: 'text-warning' },
  { label: '转化率', value: `${metrics.value.conversionRate}%`, color: 'text-primary' },
  { label: 'BSR变动', value: metrics.value.bsrChanges, color: 'text-info' }
])

/** Top 10 商品 */
const topProducts = ref([])
const productsLoading = ref(false)

/** 最近预警 */
const recentAlerts = ref([])
const alertsLoading = ref(false)

const getAlertTag = (type: string) => {
  const map = { 'STOCK': 'warning', 'PRICE': 'danger', 'REVIEW': 'info', 'BSR': 'primary' }
  return map[type] || 'info'
}

/** 图表 */
const salesChartRef = ref()
const pieChartRef = ref()
let salesChartInstance = null
let pieChartInstance = null

/** 加载店铺列表 */
const loadShopList = async () => {
  try {
    const data = await ShopApi.getShopPage({ pageNo: 1, pageSize: 100 })
    shopList.value = data.list
  } catch (error) {
    console.error('加载店铺列表失败', error)
  }
}

/** 加载指标数据 */
const loadMetrics = async () => {
  try {
    updateDateParams()
    metrics.value = await DashboardApi.getDashboardMetrics(queryParams)
  } catch (error) {
    console.error('加载指标数据失败', error)
  }
}

/** 加载概览（趋势图 + 饼图） */
const loadOverview = async () => {
  try {
    updateDateParams()
    const data = await DashboardApi.getDashboardOverview(queryParams)
    renderSalesChart(data)
    renderPieChart(data)
  } catch (error) {
    console.error('加载概览数据失败', error)
  }
}

/** 加载 Top 10 商品 */
const loadTopProducts = async () => {
  productsLoading.value = true
  try {
    updateDateParams()
    topProducts.value = await DashboardApi.getDashboardMetrics({
      ...queryParams,
      _topProducts: true
    })
  } catch (error) {
    console.error('加载Top商品失败', error)
  } finally {
    productsLoading.value = false
  }
}

/** 加载预警 */
const loadAlerts = async () => {
  alertsLoading.value = true
  try {
    recentAlerts.value = await DashboardApi.getDashboardOverview({
      ...queryParams,
      _alerts: true
    })
  } catch (error) {
    console.error('加载预警数据失败', error)
  } finally {
    alertsLoading.value = false
  }
}

/** 更新日期参数 */
const updateDateParams = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.startTime = dateRange.value[0]
    queryParams.endTime = dateRange.value[1]
  } else {
    queryParams.startTime = undefined
    queryParams.endTime = undefined
  }
}

/** 渲染销售趋势图 */
const renderSalesChart = (data) => {
  if (!salesChartRef.value) return

  if (salesChartInstance) {
    salesChartInstance.dispose()
  }

  salesChartInstance = echarts.init(salesChartRef.value)
  salesChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '订单数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.dates || [] },
    yAxis: [
      { type: 'value', name: '销售额($)' },
      { type: 'value', name: '订单数', position: 'right' }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: data.salesList || [],
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103, 194, 58, 0.1)' }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: data.ordersList || [],
        itemStyle: { color: '#409eff' }
      }
    ]
  })
}

/** 渲染饼图 */
const renderPieChart = (data) => {
  if (!pieChartRef.value) return

  if (pieChartInstance) {
    pieChartInstance.dispose()
  }

  pieChartInstance = echarts.init(pieChartRef.value)
  pieChartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ${c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '站点销售',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        data: data.marketplaceDistribution || []
      }
    ]
  })
}

/** 加载全部数据 */
const loadData = () => {
  loadMetrics()
  loadOverview()
  loadTopProducts()
  loadAlerts()
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  loadData()
})

onUnmounted(() => {
  if (salesChartInstance) salesChartInstance.dispose()
  if (pieChartInstance) pieChartInstance.dispose()
})
</script>
