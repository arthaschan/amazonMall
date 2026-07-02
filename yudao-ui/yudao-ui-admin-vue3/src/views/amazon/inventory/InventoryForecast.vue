<template>
  <ContentWrap>
    <!-- 筛选条件 -->
    <el-form :inline="true" class="-mb-15px">
      <el-form-item label="店铺">
        <el-select v-model="queryParams.shopId" placeholder="全部店铺" clearable class="!w-240px">
          <el-option v-for="shop in shopList" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="预测周期">
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
    <el-col :span="8">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-primary">{{ stats.totalSkus || 0 }}</div>
          <div class="text-gray-500 mt-5px">监控 SKU 数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-warning">{{ stats.lowStockItems || 0 }}</div>
          <div class="text-gray-500 mt-5px">低库存商品</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-danger">{{ stats.outOfStockItems || 0 }}</div>
          <div class="text-gray-500 mt-5px">缺货商品</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 库存趋势图 -->
  <ContentWrap title="库存趋势预测" class="mb-20px">
    <div ref="chartRef" class="h-400px"></div>
  </ContentWrap>

  <!-- 预测列表 -->
  <ContentWrap title="库存预测明细">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="SKU" align="center" prop="sku" width="150" />
      <el-table-column label="商品名称" align="left" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="当前库存" align="center" prop="currentStock" width="100" />
      <el-table-column label="预测库存" align="center" prop="predictedStock" width="100">
        <template #default="scope">
          <span :class="{ 'text-danger font-bold': scope.row.predictedStock <= 0 }">
            {{ scope.row.predictedStock }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="日均销量" align="center" prop="dailySales" width="100">
        <template #default="scope">
          {{ scope.row.dailySales?.toFixed(1) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="可售天数" align="center" prop="daysOfSupply" width="120">
        <template #default="scope">
          <el-tag
            :type="getDaysOfSupplyType(scope.row.daysOfSupply)"
            effect="dark"
          >
            {{ scope.row.daysOfSupply != null ? scope.row.daysOfSupply + ' 天' : '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建议补货量" align="center" prop="suggestedReorderQty" width="120">
        <template #default="scope">
          <span class="font-bold text-primary">{{ scope.row.suggestedReorderQty || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预计断货日期" align="center" prop="stockoutDate" width="150">
        <template #default="scope">
          <span :class="{ 'text-danger': scope.row.stockoutDate }">
            {{ scope.row.stockoutDate ? dateFormatter(scope.row.stockoutDate) : '-' }}
          </span>
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
import { dateFormatter } from '@/utils/formatTime'
import { InventoryForecastApi } from '@/api/amazon/inventory'
import { ShopApi } from '@/api/amazon/shop'

const shopList = ref([])
const dateRange = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  startTime: undefined,
  endTime: undefined
})

const stats = ref({
  totalSkus: 0,
  lowStockItems: 0,
  outOfStockItems: 0
})

/** 可售天数标签类型 */
const getDaysOfSupplyType = (days: number | null) => {
  if (days == null) return 'info'
  if (days <= 7) return 'danger'
  if (days <= 14) return 'warning'
  if (days <= 30) return ''
  return 'success'
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])

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

    const data = await InventoryForecastApi.getForecastPage(queryParams)
    list.value = data.list
    total.value = data.total

    // 从返回数据中提取统计信息
    if (data.stats) {
      stats.value = data.stats
    }
  } finally {
    loading.value = false
  }
}

/** 图表 */
const chartRef = ref()
let chartInstance = null

const renderChart = (data) => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: { data: ['当前库存', '预测库存', '日均销量'] },
    xAxis: { type: 'category', data: data.dates || [] },
    yAxis: [
      { type: 'value', name: '库存数量' },
      { type: 'value', name: '日均销量', position: 'right' }
    ],
    series: [
      {
        name: '当前库存',
        type: 'bar',
        data: data.currentStockList || [],
        itemStyle: { color: '#409eff' }
      },
      {
        name: '预测库存',
        type: 'line',
        data: data.predictedStockList || [],
        itemStyle: { color: '#e6a23c' },
        lineStyle: { type: 'dashed' }
      },
      {
        name: '日均销量',
        type: 'line',
        yAxisIndex: 1,
        data: data.dailySalesList || [],
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

/** 加载所有数据 */
const loadData = () => {
  getList()
  loadChartData()
}

/** 加载图表数据 */
const loadChartData = async () => {
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startTime = dateRange.value[0]
      queryParams.endTime = dateRange.value[1]
    }
    const data = await InventoryForecastApi.getForecastPage({ ...queryParams, chartOnly: true })
    if (data.chartData) {
      renderChart(data.chartData)
    }
  } catch (error) {
    console.error('加载图表数据失败', error)
  }
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  loadData()
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>
