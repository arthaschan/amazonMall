<template>
  <!-- 汇总卡片 -->
  <el-row :gutter="20" class="mb-20px">
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-primary">{{ summary.totalReviews || 0 }}</div>
          <div class="text-gray-500 mt-5px">总评论数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-warning">{{ summary.avgRating || '0.0' }}</div>
          <div class="text-gray-500 mt-5px">平均评分</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-success">{{ summary.positiveRate || '0' }}%</div>
          <div class="text-gray-500 mt-5px">好评率</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-danger">{{ summary.negativeCount || 0 }}</div>
          <div class="text-gray-500 mt-5px">差评数量</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <ContentWrap>
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
      <el-form-item label="ASIN" prop="asin">
        <el-input
          v-model="queryParams.asin"
          placeholder="请输入ASIN"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="星级" prop="starRating">
        <el-select
          v-model="queryParams.starRating"
          placeholder="全部星级"
          clearable
          class="!w-240px"
        >
          <el-option label="1星" :value="1" />
          <el-option label="2星" :value="2" />
          <el-option label="3星" :value="3" />
          <el-option label="4星" :value="4" />
          <el-option label="5星" :value="5" />
        </el-select>
      </el-form-item>
      <el-form-item label="情感" prop="sentiment">
        <el-select
          v-model="queryParams.sentiment"
          placeholder="全部情感"
          clearable
          class="!w-240px"
        >
          <el-option label="正面" value="POSITIVE" />
          <el-option label="负面" value="NEGATIVE" />
          <el-option label="中性" value="NEUTRAL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="handleSync"
          v-hasPermi="['amazon:review:sync']"
          :loading="syncLoading"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 同步评论
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 评论列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ASIN" align="center" prop="asin" width="120" />
      <el-table-column label="评论标题" align="left" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <div class="font-bold">{{ scope.row.title }}</div>
          <div class="text-gray-500 text-12px mt-5px">{{ scope.row.content }}</div>
        </template>
      </el-table-column>
      <el-table-column label="星级" align="center" prop="starRating" width="150">
        <template #default="scope">
          <el-rate v-model="scope.row.starRating" disabled show-score />
        </template>
      </el-table-column>
      <el-table-column label="情感" align="center" prop="sentiment" width="100">
        <template #default="scope">
          <el-tag :type="getSentimentTag(scope.row.sentiment)">
            {{ getSentimentLabel(scope.row.sentiment) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评论者" align="center" prop="reviewerName" width="120" />
      <el-table-column
        label="评论日期"
        align="center"
        prop="reviewDate"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="验证购买" align="center" prop="verifiedPurchase" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.verifiedPurchase" type="success" size="small">已验证</el-tag>
          <el-tag v-else type="info" size="small">未验证</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">
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
  <el-dialog v-model="detailVisible" title="评论详情" width="700px">
    <el-descriptions :column="2" border v-loading="detailLoading">
      <el-descriptions-item label="ASIN">{{ detailData.asin }}</el-descriptions-item>
      <el-descriptions-item label="评论者">{{ detailData.reviewerName }}</el-descriptions-item>
      <el-descriptions-item label="星级">
        <el-rate v-model="detailData.starRating" disabled />
      </el-descriptions-item>
      <el-descriptions-item label="情感">
        <el-tag :type="getSentimentTag(detailData.sentiment)">
          {{ getSentimentLabel(detailData.sentiment) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="验证购买">
        {{ detailData.verifiedPurchase ? '是' : '否' }}
      </el-descriptions-item>
      <el-descriptions-item label="评论日期">{{ detailData.reviewDate }}</el-descriptions-item>
      <el-descriptions-item label="标题" :span="2">{{ detailData.title }}</el-descriptions-item>
      <el-descriptions-item label="内容" :span="2">{{ detailData.content }}</el-descriptions-item>
    </el-descriptions>
    <div v-if="aiAnalysis" class="mt-20px">
      <el-divider content-position="left">AI 分析</el-divider>
      <div class="p-15px bg-gray-100 rounded">
        {{ aiAnalysis }}
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ReviewApi, AiAnalysisApi } from '@/api/amazon/review'
import { ShopApi } from '@/api/amazon/shop'

const message = useMessage()

/** 店铺列表 */
const shopList = ref([])

/** 汇总数据 */
const summary = ref({
  totalReviews: 0,
  avgRating: '0.0',
  positiveRate: '0',
  negativeCount: 0
})

/** 情感映射 */
const sentimentMap = {
  'POSITIVE': '正面',
  'NEGATIVE': '负面',
  'NEUTRAL': '中性'
}

const sentimentTagMap = {
  'POSITIVE': 'success',
  'NEGATIVE': 'danger',
  'NEUTRAL': 'info'
}

const getSentimentLabel = (sentiment: string) => sentimentMap[sentiment] || sentiment
const getSentimentTag = (sentiment: string) => sentimentTagMap[sentiment] || 'info'

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  asin: undefined,
  starRating: undefined,
  sentiment: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ReviewApi.getReviewPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 加载汇总 */
const loadSummary = async () => {
  try {
    summary.value = await ReviewApi.getReviewPage({
      ...queryParams,
      pageNo: 1,
      pageSize: 1,
      _summary: true
    })
  } catch (error) {
    console.error('加载汇总数据失败', error)
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

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  loadSummary()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 同步评论 */
const syncLoading = ref(false)
const handleSync = async () => {
  syncLoading.value = true
  try {
    await ReviewApi.syncReviews({ shopId: queryParams.shopId })
    message.success('评论同步任务已启动')
    await getList()
  } catch (error) {
    message.error('同步失败')
  } finally {
    syncLoading.value = false
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>({})
const aiAnalysis = ref('')

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  aiAnalysis.value = ''
  try {
    detailData.value = await ReviewApi.getReview(id)
    // 加载 AI 分析
    try {
      const analysis = await AiAnalysisApi.getAiAnalysis(id)
      aiAnalysis.value = analysis?.content || ''
    } catch {
      // AI 分析可选，不影响详情展示
    }
  } finally {
    detailLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
  loadSummary()
})
</script>
