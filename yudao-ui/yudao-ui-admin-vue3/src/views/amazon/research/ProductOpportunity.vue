<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="利基" prop="nicheId">
        <el-select
          v-model="queryParams.nicheId"
          placeholder="全部利基"
          clearable
          filterable
          class="!w-240px"
        >
          <el-option
            v-for="niche in nicheList"
            :key="niche.id"
            :label="niche.name"
            :value="niche.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="最低评分" prop="minScore">
        <el-input-number
          v-model="queryParams.minScore"
          :min="0"
          :max="100"
          controls-position="right"
          placeholder="0-100"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="最高评分" prop="maxScore">
        <el-input-number
          v-model="queryParams.maxScore"
          :min="0"
          :max="100"
          controls-position="right"
          placeholder="0-100"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="商品名称" align="left" prop="productName" min-width="200" show-overflow-tooltip />
      <el-table-column label="所属利基" align="center" prop="nicheName" width="150" show-overflow-tooltip />
      <el-table-column label="机会评分" align="center" prop="opportunityScore" width="180">
        <template #default="scope">
          <div class="flex items-center justify-center gap-8px">
            <el-progress
              :percentage="scope.row.opportunityScore || 0"
              :color="getScoreColor(scope.row.opportunityScore)"
              :stroke-width="16"
              :text-inside="true"
              class="!w-100px"
            />
            <el-tag
              :type="getScoreTagType(scope.row.opportunityScore)"
              size="small"
              effect="dark"
            >
              {{ getScoreLabel(scope.row.opportunityScore) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="预估月收入" align="center" prop="estimatedMonthlyRevenue" width="140">
        <template #default="scope">
          <span class="font-bold text-primary">
            ${{ scope.row.estimatedMonthlyRevenue?.toLocaleString() || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="竞争程度" align="center" prop="competition" width="120">
        <template #default="scope">
          <el-tag :type="getCompetitionType(scope.row.competition)">
            {{ getCompetitionLabel(scope.row.competition) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建议售价" align="center" prop="suggestedPrice" width="120">
        <template #default="scope">
          ${{ scope.row.suggestedPrice?.toFixed(2) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="预估利润率" align="center" prop="estimatedProfitMargin" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.estimatedProfitMargin >= 30 ? 'success' : scope.row.estimatedProfitMargin >= 15 ? '' : 'warning'">
            {{ scope.row.estimatedProfitMargin?.toFixed(1) || '-' }}%
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="推荐等级" align="center" prop="recommendation" width="120">
        <template #default="scope">
          <el-tag :type="getRecommendationType(scope.row.recommendation)" effect="dark">
            {{ getRecommendationLabel(scope.row.recommendation) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="分析时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">
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
  <el-dialog v-model="detailVisible" title="选品机会详情" width="700px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="商品名称" :span="2">{{ detailData.productName }}</el-descriptions-item>
      <el-descriptions-item label="所属利基">{{ detailData.nicheName }}</el-descriptions-item>
      <el-descriptions-item label="机会评分">
        <el-progress
          :percentage="detailData.opportunityScore || 0"
          :color="getScoreColor(detailData.opportunityScore)"
          :stroke-width="20"
          :text-inside="true"
          class="!w-200px"
        />
      </el-descriptions-item>
      <el-descriptions-item label="预估月收入">
        <span class="font-bold text-primary">${{ detailData.estimatedMonthlyRevenue?.toLocaleString() || '-' }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="预估月销量">
        {{ detailData.estimatedMonthlySales?.toLocaleString() || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="建议售价">${{ detailData.suggestedPrice?.toFixed(2) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="预估利润率">
        {{ detailData.estimatedProfitMargin?.toFixed(1) || '-' }}%
      </el-descriptions-item>
      <el-descriptions-item label="竞争程度">
        <el-tag :type="getCompetitionType(detailData.competition)">
          {{ getCompetitionLabel(detailData.competition) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="推荐等级">
        <el-tag :type="getRecommendationType(detailData.recommendation)" effect="dark">
          {{ getRecommendationLabel(detailData.recommendation) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="分析时间">
        {{ dateFormatter(detailData.createTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="推荐理由" :span="2">
        {{ detailData.reason || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="风险提示" :span="2">
        <span class="text-warning">{{ detailData.riskNote || '暂无' }}</span>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ProductOpportunityApi, NicheApi } from '@/api/amazon/research'

const message = useMessage()

/** 利基列表 */
const nicheList = ref([])

/** 竞争程度 */
const getCompetitionLabel = (level: string) => {
  const map = { 'LOW': '低', 'MEDIUM': '中', 'HIGH': '高' }
  return map[level] || level || '-'
}

const getCompetitionType = (level: string) => {
  const map = { 'LOW': 'success', 'MEDIUM': 'warning', 'HIGH': 'danger' }
  return map[level] || 'info'
}

/** 评分相关 */
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

const getScoreLabel = (score: number) => {
  if (score == null) return '-'
  if (score >= 80) return '优秀'
  if (score >= 60) return '良好'
  if (score >= 40) return '一般'
  return '较差'
}

const getScoreTagType = (score: number) => {
  if (score >= 80) return 'success'
  if (score >= 60) return ''
  if (score >= 40) return 'warning'
  return 'danger'
}

/** 推荐等级 */
const getRecommendationLabel = (level: string) => {
  const map = { 'STRONG_BUY': '强烈推荐', 'BUY': '推荐', 'HOLD': '观望', 'AVOID': '不建议' }
  return map[level] || level || '-'
}

const getRecommendationType = (level: string) => {
  const map = { 'STRONG_BUY': 'success', 'BUY': '', 'HOLD': 'warning', 'AVOID': 'danger' }
  return map[level] || 'info'
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  nicheId: undefined,
  minScore: undefined,
  maxScore: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ProductOpportunityApi.getProductOpportunityPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 加载利基列表 */
const loadNicheList = async () => {
  try {
    const data = await NicheApi.getNichePage({ pageNo: 1, pageSize: 200 })
    nicheList.value = data.list
  } catch (error) {
    console.error('加载利基列表失败', error)
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref({})

const openDetail = async (row) => {
  detailVisible.value = true
  try {
    detailData.value = await ProductOpportunityApi.getProductOpportunity(row.id)
  } catch (error) {
    // 如果详情接口不存在，使用列表行数据
    detailData.value = { ...row }
  }
}

/** 初始化 */
onMounted(() => {
  loadNicheList()
  getList()
})
</script>
