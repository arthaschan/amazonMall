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
      <el-form-item label="类目" prop="category">
        <el-input
          v-model="queryParams.category"
          placeholder="请输入类目名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="站点" prop="marketplace">
        <el-select
          v-model="queryParams.marketplace"
          placeholder="全部站点"
          clearable
          class="!w-240px"
        >
          <el-option label="美国站" value="US" />
          <el-option label="英国站" value="UK" />
          <el-option label="德国站" value="DE" />
          <el-option label="日本站" value="JP" />
          <el-option label="加拿大站" value="CA" />
          <el-option label="法国站" value="FR" />
          <el-option label="意大利站" value="IT" />
          <el-option label="西班牙站" value="ES" />
        </el-select>
      </el-form-item>
      <el-form-item label="最低价格" prop="minPrice">
        <el-input-number
          v-model="queryParams.minPrice"
          :min="0"
          :precision="2"
          controls-position="right"
          placeholder="最低价"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="最高价格" prop="maxPrice">
        <el-input-number
          v-model="queryParams.maxPrice"
          :min="0"
          :precision="2"
          controls-position="right"
          placeholder="最高价"
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
      <el-table-column label="利基名称" align="left" prop="name" min-width="200" show-overflow-tooltip />
      <el-table-column label="站点" align="center" prop="marketplace" width="100">
        <template #default="scope">
          <el-tag>{{ getMarketplaceLabel(scope.row.marketplace) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="平均价格" align="center" prop="avgPrice" width="120">
        <template #default="scope">
          ${{ scope.row.avgPrice?.toFixed(2) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="月均销量" align="center" prop="monthlySales" width="120">
        <template #default="scope">
          {{ scope.row.monthlySales?.toLocaleString() || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="竞争程度" align="center" prop="competitionLevel" width="120">
        <template #default="scope">
          <el-tag :type="getCompetitionType(scope.row.competitionLevel)">
            {{ getCompetitionLabel(scope.row.competitionLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="机会评分" align="center" prop="opportunityScore" width="140">
        <template #default="scope">
          <div class="flex items-center justify-center">
            <el-progress
              :percentage="scope.row.opportunityScore || 0"
              :color="getScoreColor(scope.row.opportunityScore)"
              :stroke-width="16"
              :text-inside="true"
              class="!w-100px"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="月均收入" align="center" prop="monthlyRevenue" width="140">
        <template #default="scope">
          ${{ scope.row.monthlyRevenue?.toLocaleString() || '-' }}
        </template>
      </el-table-column>
      <el-table-column
        label="分析时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">
            详情
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:niche:delete']"
          >
            删除
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
  <NicheDetail ref="detailRef" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { NicheApi } from '@/api/amazon/research'
import NicheDetail from './NicheDetail.vue'

const message = useMessage()
const { t } = useI18n()

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
  return map[level] || level
}

const getCompetitionType = (level: string) => {
  const map = { 'LOW': 'success', 'MEDIUM': 'warning', 'HIGH': 'danger' }
  return map[level] || 'info'
}

/** 机会评分颜色 */
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  category: undefined,
  marketplace: undefined,
  minPrice: undefined,
  maxPrice: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await NicheApi.getNichePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
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

/** 查看详情 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await NicheApi.deleteNiche(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
