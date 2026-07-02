<template>
  <el-dialog v-model="dialogVisible" title="利基市场分析详情" width="1000px" v-loading="loading">
    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab">
      <!-- 概览 -->
      <el-tab-pane label="概览" name="overview">
        <el-descriptions :column="2" border class="mt-10px">
          <el-descriptions-item label="利基名称" :span="2">{{ data.name }}</el-descriptions-item>
          <el-descriptions-item label="站点">
            <el-tag>{{ getMarketplaceLabel(data.marketplace) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类目">{{ data.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="市场规模">
            ${{ data.marketSize?.toLocaleString() || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="平均价格">
            ${{ data.avgPrice?.toFixed(2) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="月均收入">
            <span class="font-bold text-primary">${{ data.monthlyRevenue?.toLocaleString() || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="月均销量">
            {{ data.monthlySales?.toLocaleString() || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="竞争程度">
            <el-tag :type="getCompetitionType(data.competitionLevel)">
              {{ getCompetitionLabel(data.competitionLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="机会评分">
            <el-progress
              :percentage="data.opportunityScore || 0"
              :color="getScoreColor(data.opportunityScore)"
              :stroke-width="20"
              :text-inside="true"
              class="!w-200px"
            />
          </el-descriptions-item>
          <el-descriptions-item label="平均评分">
            {{ data.avgRating || '-' }} / 5.0
          </el-descriptions-item>
          <el-descriptions-item label="平均评论数">
            {{ data.avgReviewCount?.toLocaleString() || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="头部商品数">
            {{ data.topProductCount || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="分析时间">
            {{ dateFormatter(data.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- BSR 分析 -->
      <el-tab-pane label="BSR 分析" name="bsr">
        <div ref="bsrChartRef" class="h-400px mt-10px"></div>
        <el-table :data="bsrData.topProducts" border class="mt-20px">
          <el-table-column label="排名" align="center" width="80">
            <template #default="scope">
              <el-tag :type="scope.$index < 3 ? 'danger' : 'info'" round>
                {{ scope.$index + 1 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="ASIN" align="center" prop="asin" width="130" />
          <el-table-column label="商品名称" align="left" prop="title" min-width="200" show-overflow-tooltip />
          <el-table-column label="BSR" align="center" prop="bsr" width="100" />
          <el-table-column label="预估月销量" align="center" prop="estimatedMonthlySales" width="120" />
          <el-table-column label="价格" align="center" prop="price" width="100">
            <template #default="scope">
              ${{ scope.row.price?.toFixed(2) || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 财务预测 -->
      <el-tab-pane label="财务预测" name="financial">
        <el-row :gutter="20" class="mt-10px mb-20px">
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="text-center">
                <div class="text-24px font-bold text-primary">${{ financialData.monthlyRevenue?.toLocaleString() || '0' }}</div>
                <div class="text-gray-500 mt-5px">月预估收入</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="text-center">
                <div class="text-24px font-bold text-danger">${{ financialData.totalCost?.toLocaleString() || '0' }}</div>
                <div class="text-gray-500 mt-5px">月总成本</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="text-center">
                <div class="text-24px font-bold text-success">${{ financialData.monthlyProfit?.toLocaleString() || '0' }}</div>
                <div class="text-gray-500 mt-5px">月预估利润</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="text-center">
                <div class="text-24px font-bold text-warning">{{ financialData.profitMargin || '0' }}%</div>
                <div class="text-gray-500 mt-5px">利润率</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-table :data="financialData.costBreakdown" border>
          <el-table-column label="成本项" align="center" prop="name" width="200" />
          <el-table-column label="单件成本" align="center" prop="perUnit" width="150">
            <template #default="scope">
              ${{ scope.row.perUnit?.toFixed(2) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="月成本" align="center" prop="monthlyTotal" width="150">
            <template #default="scope">
              ${{ scope.row.monthlyTotal?.toLocaleString() || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="占比" align="center" width="150">
            <template #default="scope">
              <el-progress :percentage="scope.row.percentage || 0" :stroke-width="12" :text-inside="true" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 关键词 -->
      <el-tab-pane label="关键词" name="keywords">
        <el-table :data="keywordData" border class="mt-10px" v-loading="keywordLoading">
          <el-table-column label="排名" align="center" width="80">
            <template #default="scope">
              <el-tag :type="scope.$index < 3 ? 'danger' : 'info'" round>
                {{ scope.$index + 1 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关键词" align="left" prop="keyword" min-width="200" />
          <el-table-column label="搜索量" align="center" prop="searchVolume" width="120">
            <template #default="scope">
              {{ scope.row.searchVolume?.toLocaleString() || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="竞争度" align="center" prop="competition" width="100">
            <template #default="scope">
              <el-tag :type="getCompetitionType(scope.row.competition)" size="small">
                {{ getCompetitionLabel(scope.row.competition) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="CPC 竞价" align="center" prop="cpc" width="120">
            <template #default="scope">
              ${{ scope.row.cpc?.toFixed(2) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="相关度" align="center" prop="relevance" width="140">
            <template #default="scope">
              <el-progress
                :percentage="scope.row.relevance || 0"
                :stroke-width="14"
                :text-inside="true"
                :color="getScoreColor(scope.row.relevance)"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 供应商 -->
      <el-tab-pane label="供应商" name="suppliers">
        <el-table :data="supplierData" border class="mt-10px" v-loading="supplierLoading">
          <el-table-column label="供应商名称" align="left" prop="name" min-width="200" />
          <el-table-column label="地区" align="center" prop="region" width="120" />
          <el-table-column label="最小起订量" align="center" prop="moq" width="120" />
          <el-table-column label="单价范围" align="center" width="150">
            <template #default="scope">
              ${{ scope.row.minPrice?.toFixed(2) }} - ${{ scope.row.maxPrice?.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="评分" align="center" prop="rating" width="100">
            <template #default="scope">
              <el-rate v-model="scope.row.rating" disabled show-score text-color="#ff9900" />
            </template>
          </el-table-column>
          <el-table-column label="响应时间" align="center" prop="responseTime" width="120" />
          <el-table-column label="认证" align="center" prop="certifications" width="150">
            <template #default="scope">
              <el-tag v-for="cert in scope.row.certifications" :key="cert" size="small" class="mr-5px mb-5px">
                {{ cert }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="100">
            <template #default="scope">
              <el-button link type="primary" @click="viewSupplier(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { NicheApi, BsrApi, FinancialApi, KeywordResearchApi, SupplierMatchApi } from '@/api/amazon/research'

const message = useMessage()

/** 站点映射 */
const marketplaceMap = {
  'US': '美国站',
  'UK': '英国站',
  'DE': '德国站',
  'JP': '日本站',
  'CA': '加拿大站',
  'FR': '法国站',
  'IT': '意大利站',
  'ES': '西班牙站'
}

const getMarketplaceLabel = (value: string) => marketplaceMap[value] || value

/** 竞争程度 */
const getCompetitionLabel = (level: string) => {
  const map = { 'LOW': '低', 'MEDIUM': '中', 'HIGH': '高' }
  return map[level] || level || '-'
}

const getCompetitionType = (level: string) => {
  const map = { 'LOW': 'success', 'MEDIUM': 'warning', 'HIGH': 'danger' }
  return map[level] || 'info'
}

/** 评分颜色 */
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

const dialogVisible = ref(false)
const loading = ref(false)
const activeTab = ref('overview')
const data = ref({})

/** BSR 数据 */
const bsrChartRef = ref()
let bsrChartInstance = null
const bsrData = ref({ topProducts: [] })

/** 财务数据 */
const financialData = ref({
  monthlyRevenue: 0,
  totalCost: 0,
  monthlyProfit: 0,
  profitMargin: '0',
  costBreakdown: []
})

/** 关键词数据 */
const keywordData = ref([])
const keywordLoading = ref(false)

/** 供应商数据 */
const supplierData = ref([])
const supplierLoading = ref(false)

/** 加载 BSR 数据 */
const loadBsrData = async (nicheId: number) => {
  try {
    const result = await BsrApi.getBsrData({ nicheId })
    bsrData.value = result

    // 渲染 BSR 趋势图
    nextTick(() => {
      renderBsrChart(result)
    })
  } catch (error) {
    console.error('加载 BSR 数据失败', error)
  }
}

const renderBsrChart = (result) => {
  if (!bsrChartRef.value) return

  if (bsrChartInstance) {
    bsrChartInstance.dispose()
  }

  bsrChartInstance = echarts.init(bsrChartRef.value)
  bsrChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['BSR 排名', '预估销量'] },
    xAxis: { type: 'category', data: result.dates || [] },
    yAxis: [
      { type: 'value', name: 'BSR 排名', inverse: true },
      { type: 'value', name: '预估销量', position: 'right' }
    ],
    series: [
      {
        name: 'BSR 排名',
        type: 'line',
        data: result.bsrValues || [],
        itemStyle: { color: '#409eff' },
        smooth: true
      },
      {
        name: '预估销量',
        type: 'bar',
        yAxisIndex: 1,
        data: result.estimatedSales || [],
        itemStyle: { color: '#67c23a' }
      }
    ]
  })
}

/** 加载财务数据 */
const loadFinancialData = async (nicheId: number) => {
  try {
    financialData.value = await FinancialApi.getFinancialProjection({ nicheId })
  } catch (error) {
    console.error('加载财务数据失败', error)
  }
}

/** 加载关键词数据 */
const loadKeywordData = async (nicheId: number) => {
  keywordLoading.value = true
  try {
    const result = await KeywordResearchApi.getKeywordResearch({ nicheId })
    keywordData.value = result.keywords || result || []
  } finally {
    keywordLoading.value = false
  }
}

/** 加载供应商数据 */
const loadSupplierData = async (nicheId: number) => {
  supplierLoading.value = true
  try {
    const result = await SupplierMatchApi.getSupplierMatch({ nicheId })
    supplierData.value = result.suppliers || result || []
  } finally {
    supplierLoading.value = false
  }
}

/** 供应商详情 */
const viewSupplier = (row) => {
  message.info('供应商详情功能开发中')
}

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  activeTab.value = 'overview'
  loading.value = true
  data.value = {}
  bsrData.value = { topProducts: [] }
  financialData.value = { monthlyRevenue: 0, totalCost: 0, monthlyProfit: 0, profitMargin: '0', costBreakdown: [] }
  keywordData.value = []
  supplierData.value = []

  try {
    data.value = await NicheApi.getNiche(id)
    // 并行加载各 Tab 数据
    loadBsrData(id)
    loadFinancialData(id)
    loadKeywordData(id)
    loadSupplierData(id)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })

onUnmounted(() => {
  if (bsrChartInstance) {
    bsrChartInstance.dispose()
  }
})
</script>
