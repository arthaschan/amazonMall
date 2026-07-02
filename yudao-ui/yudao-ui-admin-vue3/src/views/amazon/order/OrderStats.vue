<template>
  <ContentWrap>
    <!-- 筛选条件 -->
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
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 统计卡片 -->
  <el-row :gutter="20" class="mb-20px">
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-primary">{{ overview.totalOrders || 0 }}</div>
          <div class="text-gray-500 mt-5px">总订单数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-success">${{ overview.totalSales || '0.00' }}</div>
          <div class="text-gray-500 mt-5px">总销售额</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold">{{ overview.avgOrderValue || '$0.00' }}</div>
          <div class="text-gray-500 mt-5px">平均客单价</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-warning">{{ overview.refundRate || '0%' }}</div>
          <div class="text-gray-500 mt-5px">退款率</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 销售趋势图 -->
  <ContentWrap title="销售趋势" class="mb-20px">
    <div ref="chartRef" class="h-400px"></div>
  </ContentWrap>

  <!-- 热销商品 -->
  <ContentWrap title="热销商品 TOP 10">
    <el-table :data="topProducts" v-loading="productsLoading">
      <el-table-column label="排名" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.$index < 3 ? 'danger' : 'info'" round>
            {{ scope.$index + 1 }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="SKU" align="center" prop="sku" width="150" />
      <el-table-column label="商品名称" align="left" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="销量" align="center" prop="quantity" width="120" />
      <el-table-column label="销售额" align="center" prop="sales" width="150">
        <template #default="scope">
          ${{ scope.row.sales }}
        </template>
      </el-table-column>
      <el-table-column label="占比" align="center" width="120">
        <template #default="scope">
          {{ scope.row.percentage }}%
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script setup lang="ts">
import { OrderStatsApi } from '@/api/amazon/order'
import { ShopApi } from '@/api/amazon/shop'

const shopList = ref([])
const dateRange = ref([])
const queryParams = reactive({
  shopId: undefined,
  startTime: undefined,
  endTime: undefined
})

const overview = ref({
  totalOrders: 0,
  totalSales: '0.00',
  avgOrderValue: '$0.00',
  refundRate: '0%'
})

const topProducts = ref([])
const productsLoading = ref(false)

const chartRef = ref()
let chartInstance = null

const loadShopList = async () => {
  try {
    const data = await ShopApi.getShopPage({ pageNo: 1, pageSize: 100 })
    shopList.value = data.list
  } catch (error) {
    console.error('加载店铺列表失败', error)
  }
}

const loadOverview = async () => {
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startTime = dateRange.value[0]
      queryParams.endTime = dateRange.value[1]
    } else {
      queryParams.startTime = undefined
      queryParams.endTime = undefined
    }
    overview.value = await OrderStatsApi.getOverview(queryParams)
  } catch (error) {
    console.error('加载概览数据失败', error)
  }
}

const loadSalesTrend = async () => {
  try {
    const data = await OrderStatsApi.getSalesTrend(queryParams)
    renderChart(data)
  } catch (error) {
    console.error('加载趋势数据失败', error)
  }
}

const loadTopProducts = async () => {
  productsLoading.value = true
  try {
    topProducts.value = await OrderStatsApi.getTopProducts(queryParams)
  } finally {
    productsLoading.value = false
  }
}

const renderChart = (data) => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '销售额'] },
    xAxis: { type: 'category', data: data.dates || [] },
    yAxis: [
      { type: 'value', name: '订单数' },
      { type: 'value', name: '销售额($)', position: 'right' }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: data.orderCounts || [],
        itemStyle: { color: '#409eff' }
      },
      {
        name: '销售额',
        type: 'line',
        yAxisIndex: 1,
        data: data.salesAmounts || [],
        itemStyle: { color: '#67c23a' }
      }
    ]
  })
}

const loadData = () => {
  loadOverview()
  loadSalesTrend()
  loadTopProducts()
}

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
