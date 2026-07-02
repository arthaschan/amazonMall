<template>
  <ContentWrap>
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="text-center">
            <div class="text-24px font-bold">{{ stats.totalCalls || 0 }}</div>
            <div class="text-gray-500 mt-5px">总调用次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="text-center">
            <div class="text-24px font-bold text-success">{{ stats.successRate || '0%' }}</div>
            <div class="text-gray-500 mt-5px">成功率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="text-center">
            <div class="text-24px font-bold">{{ stats.avgResponseTime || '0ms' }}</div>
            <div class="text-gray-500 mt-5px">平均响应时间</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="text-center">
            <div class="text-24px font-bold text-danger">{{ stats.errorCount || 0 }}</div>
            <div class="text-gray-500 mt-5px">错误次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="店铺" prop="shopId">
        <el-select
          v-model="queryParams.shopId"
          placeholder="全部店铺"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="shop in shopList"
            :key="shop.id"
            :label="shop.name"
            :value="shop.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="API类型" prop="apiType">
        <el-select
          v-model="queryParams.apiType"
          placeholder="全部类型"
          clearable
          class="!w-240px"
        >
          <el-option label="Orders" value="Orders" />
          <el-option label="Catalog" value="Catalog" />
          <el-option label="Inventory" value="Inventory" />
          <el-option label="Reports" value="Reports" />
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
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- API 调用日志列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="logList">
      <el-table-column label="店铺" align="center" prop="shopName" width="120" />
      <el-table-column label="API类型" align="center" prop="apiType" width="120" />
      <el-table-column label="请求方法" align="center" prop="httpMethod" width="100" />
      <el-table-column label="请求URL" align="center" prop="requestUrl" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态码" align="center" prop="statusCode" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.statusCode === 200 ? 'success' : 'danger'">
            {{ scope.row.statusCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="响应时间" align="center" prop="responseTime" width="120">
        <template #default="scope">
          {{ scope.row.responseTime }}ms
        </template>
      </el-table-column>
      <el-table-column
        label="调用时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetail(scope.row)">
            详情
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
  <el-dialog v-model="detailVisible" title="API调用详情" width="600px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="请求URL" :span="2">
        {{ detailData.requestUrl }}
      </el-descriptions-item>
      <el-descriptions-item label="请求方法">
        {{ detailData.httpMethod }}
      </el-descriptions-item>
      <el-descriptions-item label="状态码">
        {{ detailData.statusCode }}
      </el-descriptions-item>
      <el-descriptions-item label="响应时间">
        {{ detailData.responseTime }}ms
      </el-descriptions-item>
      <el-descriptions-item label="调用时间">
        {{ dateFormatter(detailData.createTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="请求参数" :span="2">
        <pre class="whitespace-pre-wrap">{{ detailData.requestBody }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="响应内容" :span="2">
        <pre class="whitespace-pre-wrap">{{ detailData.responseBody }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="错误信息" :span="2" v-if="detailData.errorMessage">
        <pre class="whitespace-pre-wrap text-danger">{{ detailData.errorMessage }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ShopApi, ApiMonitorApi } from '@/api/amazon/shop'

const message = useMessage()

/** 统计数据 */
const stats = ref({
  totalCalls: 0,
  successRate: '0%',
  avgResponseTime: '0ms',
  errorCount: 0
})

/** 店铺列表 */
const shopList = ref([])

/** 查询日志列表 */
const loading = ref(true)
const total = ref(0)
const logList = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  apiType: undefined,
  startTime: undefined,
  endTime: undefined
})
const queryFormRef = ref()
const dateRange = ref([])

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

    const data = await ApiMonitorApi.getApiLogPage(queryParams)
    logList.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 加载统计数据 */
const loadStats = async () => {
  try {
    const data = await ApiMonitorApi.getApiStats(queryParams)
    stats.value = data
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
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

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  loadStats()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  dateRange.value = []
  handleQuery()
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref({})

const showDetail = (row) => {
  detailData.value = row
  detailVisible.value = true
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
  loadStats()
})
</script>
