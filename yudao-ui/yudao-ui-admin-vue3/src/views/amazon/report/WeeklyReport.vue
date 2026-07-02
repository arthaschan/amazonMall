<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="报告标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入报告标题"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="handleGenerate"
          v-hasPermi="['amazon:report:generate']"
          :loading="generateLoading"
        >
          <Icon icon="ep:document-add" class="mr-5px" /> 生成周报
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 周报列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="报告标题" align="left" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="报告周期" align="center" width="220">
        <template #default="scope">
          {{ scope.row.startDate }} ~ {{ scope.row.endDate }}
        </template>
      </el-table-column>
      <el-table-column
        label="生成时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTag(scope.row.status)">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openDetail(scope.row.id)"
            :disabled="scope.row.status !== 'COMPLETED'"
          >
            查看详情
          </el-button>
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

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" title="周报详情" width="900px" top="5vh">
    <div v-loading="detailLoading">
      <!-- 摘要 -->
      <el-divider content-position="left">报告摘要</el-divider>
      <div class="p-15px bg-gray-50 rounded mb-20px">
        {{ detailData.summary || '暂无摘要' }}
      </div>

      <!-- 关键指标对比 -->
      <el-divider content-position="left">关键指标对比（本周 vs 上周）</el-divider>
      <el-row :gutter="15" class="mb-20px">
        <el-col :span="6" v-for="metric in metricsComparison" :key="metric.label">
          <el-card shadow="hover" class="text-center">
            <div class="text-14px text-gray-500 mb-5px">{{ metric.label }}</div>
            <div class="text-20px font-bold">{{ metric.current }}</div>
            <div class="text-12px mt-5px" :class="metric.change > 0 ? 'text-success' : 'text-danger'">
              {{ metric.change > 0 ? '+' : '' }}{{ metric.change }}%
              <span class="text-gray-400">vs 上周 {{ metric.previous }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Top 商品 -->
      <el-divider content-position="left">本周 Top 商品</el-divider>
      <el-table :data="detailData.topProducts || []" class="mb-20px">
        <el-table-column label="排名" align="center" width="70">
          <template #default="scope">
            <el-tag :type="scope.$index < 3 ? 'danger' : 'info'" round>
              {{ scope.$index + 1 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="SKU" align="center" prop="sku" width="120" />
        <el-table-column label="商品名称" align="left" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="销售额" align="center" prop="sales" width="110">
          <template #default="scope">${{ scope.row.sales }}</template>
        </el-table-column>
        <el-table-column label="订单数" align="center" prop="orders" width="80" />
      </el-table>

      <!-- AI 建议 -->
      <el-divider content-position="left">运营建议</el-divider>
      <div class="p-15px bg-blue-50 rounded">
        <div v-if="detailData.recommendations" class="whitespace-pre-wrap">
          {{ detailData.recommendations }}
        </div>
        <div v-else class="text-gray-400">暂无建议</div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { WeeklyReportApi } from '@/api/amazon/report'

const message = useMessage()

const statusMap = {
  'PENDING': '生成中',
  'COMPLETED': '已完成',
  'FAILED': '生成失败'
}

const statusTagMap = {
  'PENDING': 'warning',
  'COMPLETED': 'success',
  'FAILED': 'danger'
}

const getStatusLabel = (status: string) => statusMap[status] || status
const getStatusTag = (status: string) => statusTagMap[status] || 'info'

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  title: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await WeeklyReportApi.getWeeklyReportPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 生成周报 */
const generateLoading = ref(false)
const handleGenerate = async () => {
  try {
    await message.confirm('确定要生成本周周报吗？')
    generateLoading.value = true
    await WeeklyReportApi.generateWeeklyReport({})
    message.success('周报生成任务已启动')
    await getList()
  } catch {} finally {
    generateLoading.value = false
  }
}

/** 详情 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>({})

const metricsComparison = computed(() => {
  const m = detailData.value.metrics || {}
  return [
    {
      label: '销售额',
      current: `$${m.currentSales || '0.00'}`,
      previous: `$${m.previousSales || '0.00'}`,
      change: m.salesChange || 0
    },
    {
      label: '订单数',
      current: m.currentOrders || 0,
      previous: m.previousOrders || 0,
      change: m.ordersChange || 0
    },
    {
      label: '利润',
      current: `$${m.currentProfit || '0.00'}`,
      previous: `$${m.previousProfit || '0.00'}`,
      change: m.profitChange || 0
    },
    {
      label: '转化率',
      current: `${m.currentConversion || '0.00'}%`,
      previous: `${m.previousConversion || '0.00'}%`,
      change: m.conversionChange || 0
    }
  ]
})

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await WeeklyReportApi.getWeeklyReport(id)
  } finally {
    detailLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
